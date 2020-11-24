//
//  ViewController.swift
//  FlowAR
//
//  Created by 周舒意 on 2020/10/20.
//  Copyright © 2020 周舒意. All rights reserved.
//

import UIKit
import SceneKit
import ARKit
import Vision
import ClassKit

class ViewController: UIViewController, ARSCNViewDelegate, ARSessionDelegate, URLSessionDelegate {

    @IBOutlet var sceneView: ARSCNView!
    
    @IBOutlet weak var blurView: UIVisualEffectView!
    
    var qrRequests = [VNRequest]()
    var detectedDataAnchor: [String: ARAnchor?] = [:] // QR location
    var message: String! // QR Message === ID
    var detectedDataResult: [String: [String: Any]] = [:] // [ID: ["AssetId":"sfsdf", "AssetName": "sfdsfd"]]
    var cabinet: String! // cabinet name -> getAssetByName
    
    /// The view controller that displays the status and "restart experience" UI.
    lazy var statusViewController: StatusViewController = {
        return children.lazy.compactMap({ $0 as? StatusViewController }).first!
    }()
    /// A serial queue for thread safety when modifying the SceneKit node graph.
    let updateQueue = DispatchQueue(label: Bundle.main.bundleIdentifier! +
        ".serialSceneKitQueue")
    
    /// Convenience accessor for the session owned by ARSCNView.
    var session: ARSession {
        return sceneView.session
    }
    
    var lastAddedAnchor: ARAnchor? // 最近添加的QRcode的anchor
    var processing = false // QR code image process

    
    let host = "https://202.121.180.32/"      // FLOWGATE_HOST
    var password = "QWxv_3arJ70gl"         // FLOWGATE_PASSWORD
    var username = "API"
    var current_token: [String: Any] = [:]
    let items = [
        "assetName",
        "assetNumber",
        "assetSource",
        "category",
        "subCategory",
        "manufacturer",
        "model",
        "tag",
        "cabinetName"] // 看情况加
    var fetch_result: [String: Any] = [:] // data for one cabinet
    
    lazy var cabinet_b = false {// is cabinet detected
        didSet{
            if(cabinet_b) {self.startFigure()}
        }
    }
    
    // MARK: - View Controller Life Cycle
    override func viewDidLoad() {
        super.viewDidLoad()
        Swift.print(getFlowgateToken())
        // Set the view's delegate
        sceneView.delegate = self
        sceneView.session.delegate=self
//        sceneView.debugOptions = [ARSCNDebugOptions.showWorldOrigin]
     
        // Hook up status view controller callback(s).
        statusViewController.restartExperienceHandler = { [unowned self] in
            self.restartExperience()
        }
        startQrCodeDetection()
    
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        // Prevent the screen from being dimmed to avoid interuppting the AR experience.
        UIApplication.shared.isIdleTimerDisabled = true

        // Start the AR experience
        resetTracking()
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)

        session.pause()
    }
    
    // MARK: - Session management (Image detection setup)
    
    /// Prevents restarting the session while a restart is in progress.
    var isRestartAvailable = true

    /// Creates a new AR configuration to run on the `session`.
    /// - Tag: ARReferenceImage-Loading
    func startFigure(){
        guard let referenceImages = ARReferenceImage.referenceImages(inGroupNamed: "AR Resources", bundle: nil) else {
            fatalError("Missing expected asset catalog resources.")
        }
        let configuration = ARWorldTrackingConfiguration()
        configuration.detectionImages = referenceImages
        session.run(configuration)
    }
    func resetTracking() {
        
//        guard let referenceImages = ARReferenceImage.referenceImages(inGroupNamed: "AR Resources", bundle: nil) else {
//            fatalError("Missing expected asset catalog resources.")
//        }
//
        let configuration = ARWorldTrackingConfiguration()
//        configuration.detectionImages = referenceImages
        session.run(configuration, options: [.resetTracking, .removeExistingAnchors])

        statusViewController.scheduleMessage("Look around to detect server", inSeconds: 7.5, messageType: .contentPlacement)
        cabinet_b = false;
    }


    func startQrCodeDetection() {
        
        // Create a Barcode Detection Request
        let request = VNDetectBarcodesRequest(completionHandler: self.requestHandler)
        // Set it to recognize QR code only
        request.symbologies = [.Aztec, .Code39, .Code39Checksum, .Code39FullASCII, .Code39FullASCIIChecksum, .Code93, .Code93i, .Code128, .DataMatrix, .EAN8,
                               .EAN13, .I2of5, .I2of5Checksum, .ITF14, .PDF417, .UPCE]
        self.qrRequests = [request]
    }
    
    func requestHandler(request: VNRequest, error: Error?) {
        // Get the first result out of the results, if there are any
        if let results = request.results, let result = results.first as? VNBarcodeObservation {
            guard let message=result.payloadStringValue else {return}
            self.message = message
            Swift.print(self.message ?? "No message.")
            // Get the bounding box for the bar code and find the center
            var rect = result.boundingBox
            // TODO: Draw it
            // Flip coordinates
            rect = rect.applying(CGAffineTransform(scaleX: 1, y: -1))
            rect = rect.applying(CGAffineTransform(translationX: 0, y: 1))
            // Get center
            let center = CGPoint(x: rect.midX, y: rect.midY)
            
            DispatchQueue.main.async {
                self.hitTestQrCode(center: center, message: message)
                self.processing = false
            }
        } else {
            self.processing = false
        }
    }
    
    func hitTestQrCode(center: CGPoint, message: String) {
//        print(detectedDataResult)
        if let hitTestResults = sceneView?.hitTest(center, types: [.featurePoint] ),
            let hitTestResult = hitTestResults.first {
            if let detectedDataAnchor = self.detectedDataAnchor[message], // ID的anchor
               let node = self.sceneView.node(for: detectedDataAnchor!) {
                _ = node.position
                node.transform = SCNMatrix4(hitTestResult.worldTransform)
            } else {
                // Create an anchor. The node will be created in delegate methods
                self.detectedDataAnchor[message] = ARAnchor(transform: hitTestResult.worldTransform)
                self.getAssetByID(ID: message)
                self.lastAddedAnchor = self.detectedDataAnchor[message] as? ARAnchor
            }
        }
    }
    
    public func session(_ session: ARSession, didUpdate frame: ARFrame) {
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                if self.processing {
                    return
                }
                self.processing = true
                // Create a request handler using the captured image from the ARFrame
                let imageRequestHandler = VNImageRequestHandler(cvPixelBuffer: frame.capturedImage,
                                                                options: [:])
                // Process the request
                try imageRequestHandler.perform(self.qrRequests)
            } catch {
                
            }
        }
    }
    
    func generate_text(_ text: String, _ x: Float, _ y: Float, _ z: Float,
                       _ bold: Bool=false, _ size: Float=1, _ center: Bool=false) -> SCNNode {
        let mesages = SCNText(string: text, extrusionDepth: 0) // SCN开头的geometry
        if(bold) {mesages.font = UIFont(name:"HelveticaNeue-Bold", size: 12)}
        else {mesages.font = UIFont(name:"HelveticaNeue", size: 12)}
        let material = SCNMaterial()
        material.diffuse.contents = UIColor.black
        mesages.materials = [material]
        
        let messageNode = SCNNode(geometry: mesages)
        messageNode.scale = SCNVector3Make( 0.001*size, 0.001*size, 0.001*size)
        // 锚点
        // set pivot of left top point
        var minVec = SCNVector3Zero
        var maxVec = SCNVector3Zero
        (minVec, maxVec) =  messageNode.boundingBox
        if(center){
            messageNode.pivot = SCNMatrix4MakeTranslation(
                (minVec.x + maxVec.x)/2,
                maxVec.y,
                minVec.z
            )
            messageNode.position = messageNode.position + SCNVector3(0, y, z)
        }
        else {messageNode.pivot = SCNMatrix4MakeTranslation(
            minVec.x,
            maxVec.y,
            minVec.z
        )
        messageNode.position = messageNode.position + SCNVector3(x, y, z)
        }
        
        return messageNode
    }
    
    // Anchor(定位) ->Node(Anchor) object -> node加进你要的返回的node
    // Geometry     _^

    // MARK: - ARSCNViewDelegate

    
    // Override to create and configure nodes for anchors added to the view's session.
    func renderer(_ renderer: SCNSceneRenderer, nodeFor anchor: ARAnchor) -> SCNNode? {

        if self.lastAddedAnchor?.identifier == anchor.identifier {

            DispatchQueue.main.async {
                self.statusViewController.cancelAllScheduledMessages()
                self.statusViewController.showMessage("Detected a bar code")
            }
            let node = SCNNode()
            guard let ID = self.message else { return node }
            let result = strFormat(content: detectedDataResult[ID]! as [String: Any])
            let left_message = generate_text(result["type"]!, -0.10, 0.05, 0.01)
            let right_message = generate_text(result["content"]!, -0.02, 0.05, 0.01)
            let title_message = generate_text(result["title"]!, -0.065, 0.08, 0.01, true, 2, true)
            node.addChildNode(left_message)
            node.addChildNode(right_message)
            node.addChildNode(title_message)


            let plane = SCNPlane(width: 0.25, height: 0.2)
            plane.cornerRadius = 0.02
            let planeNode = SCNNode(geometry: plane)
            planeNode.eulerAngles.x = 0
            planeNode.opacity = 0.4
            node.addChildNode(planeNode)
            return node

        } else if let imageAnchor = anchor  as? ARImageAnchor{
            let referenceImage = imageAnchor.referenceImage
            let node = SCNNode()
            updateQueue.async {
                guard let path = Bundle.main.path(forResource: "wireframe_shader", ofType: "metal", inDirectory: "art.scnassets"),
                    let shader = try? String(contentsOfFile: path, encoding: .utf8) else {
                    print(Bundle.main.path(forResource: "wireframe_shader", ofType: "metal", inDirectory: "Assets.xcassets") ?? "nothing")
                        print("faile to open")
                        return
                }
                // Create a plane to visualize the initial position of the detected image.
                let wireFrame = SCNNode()
                let box = SCNBox(width: referenceImage.physicalSize.width, height: 0, length: referenceImage.physicalSize.height, chamferRadius: 0)
                box.firstMaterial?.diffuse.contents = UIColor.red
                box.firstMaterial?.isDoubleSided = true
                box.firstMaterial?.shaderModifiers = [.surface: shader]
                wireFrame.geometry = box
                
                let result = self.strFormat(content: self.fetch_result as [String: Any])
                let left_message = self.generate_text(result["type"]!, Float(2*referenceImage.physicalSize.width+0.11), 0.01, -0.05)// (x,y,z: length,depth,height)
                let right_message = self.generate_text(result["content"]!, Float(2*referenceImage.physicalSize.width+0.03), 0.01, -0.05)
                let title_message = self.generate_text(result["title"]!, Float(2*referenceImage.physicalSize.width+0.07), 0.01, -0.08, true, 2)
                node.addChildNode(left_message)
                node.addChildNode(right_message)
                node.addChildNode(title_message)


                let plane = SCNPlane(width: 0.25, height: 0.2)
                plane.cornerRadius = 0.02
                let planeNode = SCNNode(geometry: plane)
                planeNode.eulerAngles.x = 0
                planeNode.opacity = 0.4
                planeNode.position = SCNVector3Make(Float(2*referenceImage.physicalSize.width+0.115), 0.009, -0.02)
                node.addChildNode(planeNode)
                
                /*
                 `SCNPlane` is vertically oriented in its local coordinate space, but
                 `ARImageAnchor` assumes the image is horizontal in its local space, so
                 rotate the plane to match.
//                 */
                left_message.eulerAngles.x = -.pi / 2
                right_message.eulerAngles.x = -.pi / 2
                title_message.eulerAngles.x = -.pi / 2
                planeNode.eulerAngles.x = -.pi / 2
//                planeNode.runAction(imageHighlightAction)
                node.addChildNode(wireFrame)
                
            }
            DispatchQueue.main.async {
                self.statusViewController.cancelAllScheduledMessages()
                self.statusViewController.showMessage("Detected a cabinet")
            }
            return node
        }
        return nil
    }
    
    // MARK: - ARSCNViewDelegate (Image detection results)
    /// - Tag: ARImageAnchor-Visualizing

    var imageHighlightAction: SCNAction {
        return .sequence([
            .wait(duration: 0.25),
            .fadeOpacity(to: 0.85, duration: 0.25),
            .fadeOpacity(to: 0.15, duration: 0.25),
            .fadeOpacity(to: 0.85, duration: 0.25),
            .fadeOut(duration: 0.5),
            .removeFromParentNode()
        ])
    }
}

extension SCNVector3 {
    static func + (left: SCNVector3, right: SCNVector3) -> SCNVector3 {
        return SCNVector3Make(left.x + right.x, left.y + right.y, left.z + right.z)
    }
}
