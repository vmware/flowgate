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
    var detectedDataAnchor: [String: ARAnchor?] = [:]
    var message: String!
    var detectedDataResult: [String: [String: Any]] = [:]
    
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
    
    var lastAddedAnchor: ARAnchor?
    var processing = false

    
    var host = "https://202.121.180.32/"      // FLOWGATE_HOST
    var password = "QWxv_3arJ70gl"         // FLOWGATE_PASSWORD
    var username = "API"
    var current_token: [String: Any] = [:]
    var semaphore = DispatchSemaphore(value: 1)
    let items = [
        "assetName",
        "assetNumber",
        "assetSource",
        "category"]
    var fetch_result: [String: Any] = [:]
    
    // MARK: - View Controller Life Cycle
    override func viewDidLoad() {
        super.viewDidLoad()
        Swift.print(getFlowgateToken())
        // Set the view's delegate
        sceneView.delegate = self
        sceneView.session.delegate=self
//        sceneView.showsStatistics = true
//        sceneView.debugOptions = [ARSCNDebugOptions.showWorldOrigin,
//                                  ARSCNDebugOptions.showFeaturePoints]
     
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
    func resetTracking() {
        
        guard let referenceImages = ARReferenceImage.referenceImages(inGroupNamed: "AR Resources", bundle: nil) else {
            fatalError("Missing expected asset catalog resources.")
        }
        
        let configuration = ARWorldTrackingConfiguration()
        configuration.detectionImages = referenceImages
        session.run(configuration, options: [.resetTracking, .removeExistingAnchors])

        statusViewController.scheduleMessage("Look around to detect images", inSeconds: 7.5, messageType: .contentPlacement)
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
            if let detectedDataAnchor = self.detectedDataAnchor[message],
               let node = self.sceneView.node(for: detectedDataAnchor!) {
                _ = node.position
                node.transform = SCNMatrix4(hitTestResult.worldTransform)
            } else {
                // Create an anchor. The node will be created in delegate methods
                self.detectedDataAnchor[message] = ARAnchor(transform: hitTestResult.worldTransform)
                self.getAssetByID(ID: message)
                print(detectedDataResult[message] ?? "no message")
                self.lastAddedAnchor = self.detectedDataAnchor[message] as? ARAnchor
//                self.sceneView.session.add(anchor: self.detectedDataAnchor[message]!!)
            }
        }
    }
    
//    override func viewWillAppear(_ animated: Bool) {
//        super.viewWillAppear(animated)
//        guard let referenceImages = ARReferenceImage.referenceImages(inGroupNamed: "AR Resources", bundle: nil) else {
//            fatalError("Missing expected asset catalog resources.")
//        }
//
//        // Create a session configuration
//        let configuration = ARWorldTrackingConfiguration()
//        configuration.detectionImages = referenceImages
//
//        // Run the view's session
//        sceneView.session.run(configuration)
//    }
    
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
    
    func generate_text(_ text: String, _ x: Float, _ y: Float, _ z: Float, _ bold: Bool=false, _ size: Float=1) -> SCNNode {
        let mesages = SCNText(string: text, extrusionDepth: 0)
        if(bold) {mesages.font = UIFont(name:"HelveticaNeue-Bold", size: 12)}
        else {mesages.font = UIFont(name:"HelveticaNeue", size: 12)}
        let material = SCNMaterial()
        material.diffuse.contents = UIColor.black
        mesages.materials = [material]
        
        let messageNode = SCNNode(geometry: mesages)
        messageNode.scale = SCNVector3Make( 0.001*size, 0.001*size, 0.001*size)
        // set pivot of left top point
        var minVec = SCNVector3Zero
        var maxVec = SCNVector3Zero
        (minVec, maxVec) =  messageNode.boundingBox
        messageNode.pivot = SCNMatrix4MakeTranslation(
            minVec.x,
            maxVec.y,
            minVec.z
        )
        messageNode.position = messageNode.position + SCNVector3(x, y, z)
        return messageNode
    }

    // MARK: - ARSCNViewDelegate

    
    // Override to create and configure nodes for anchors added to the view's session.
//    func renderer(_ renderer: SCNSceneRenderer, nodeFor anchor: ARAnchor) -> SCNNode? {
//
//        if self.lastAddedAnchor?.identifier == anchor.identifier {
//
//            let node = SCNNode()
//            guard let ID = self.message else { return node }
////            while self.detectedDataResult[ID]==nil {
////                print("wait")
////            }
//            let result = strFormat(ID: ID)
//            let left_message = generate_text(result["type"]!, -0.08, 0.05, 0.01, true)
//            let right_message = generate_text(result["content"]!, 0.02, 0.05, 0.01)
//            let title_message = generate_text(result["title"]!, -0.06, 0.08, 0.01, true, 2)
//            node.addChildNode(left_message)
//            node.addChildNode(right_message)
//            node.addChildNode(title_message)
//
//
//            let plane = SCNPlane(width: 0.2, height: 0.2)
//            plane.cornerRadius = 0.02
//            let planeNode = SCNNode(geometry: plane)
//            planeNode.eulerAngles.x = 0
//            planeNode.opacity = 0.4
//            node.addChildNode(planeNode)
//
//
////            node.addChildNode(addView())
//            return node
//
//        }
//        return nil
//    }
    
    // MARK: - ARSCNViewDelegate (Image detection results)
    /// - Tag: ARImageAnchor-Visualizing
    func renderer(_ renderer: SCNSceneRenderer, didAdd node: SCNNode, for anchor: ARAnchor){
        print("detected")
        if let imageAnchor = anchor  as? ARImageAnchor{
            let referenceImage = imageAnchor.referenceImage
            updateQueue.async {
                
                // Create a plane to visualize the initial position of the detected image.
                let plane = SCNPlane(width: referenceImage.physicalSize.width,
                                     height: referenceImage.physicalSize.height)
                let planeNode = SCNNode(geometry: plane)
                planeNode.opacity = 0.25
                
                /*
                 `SCNPlane` is vertically oriented in its local coordinate space, but
                 `ARImageAnchor` assumes the image is horizontal in its local space, so
                 rotate the plane to match.
                 */
                planeNode.eulerAngles.x = -.pi / 2
                
                /*
                 Image anchors are not tracked after initial detection, so create an
                 animation that limits the duration for which the plane visualization appears.
                 */
                planeNode.runAction(self.imageHighlightAction)
                
                // Add the plane visualization to the scene.
                node.addChildNode(planeNode)
            }
            DispatchQueue.main.async {
                let imageName = referenceImage.name ?? ""
                self.statusViewController.cancelAllScheduledMessages()
                self.statusViewController.showMessage("Detected image “\(imageName)”")
            }
        } else if self.lastAddedAnchor?.identifier == anchor.identifier {
            
            let node = SCNNode()
            guard let ID = self.message else { return}
//            while self.detectedDataResult[ID]==nil {
//                print("wait")
//            }
            let result = strFormat(ID: ID)
            let left_message = generate_text(result["type"]!, -0.08, 0.05, 0.01, true)
            let right_message = generate_text(result["content"]!, 0.02, 0.05, 0.01)
            let title_message = generate_text(result["title"]!, -0.06, 0.08, 0.01, true, 2)
            node.addChildNode(left_message)
            node.addChildNode(right_message)
            node.addChildNode(title_message)


            let plane = SCNPlane(width: 0.2, height: 0.2)
            plane.cornerRadius = 0.02
            let planeNode = SCNNode(geometry: plane)
            planeNode.eulerAngles.x = 0
            planeNode.opacity = 0.4
            node.addChildNode(planeNode)
//            node.addChildNode(addView())
        }
    }
    func addView() ->SCNNode{
        
        let skScene = SKScene(size: CGSize(width: 200, height: 200))
        skScene.backgroundColor = UIColor.clear
        
        let rectangle = SKShapeNode(rect: CGRect(x: 0, y: 0, width: 200, height: 200), cornerRadius: 10)
        rectangle.fillColor = #colorLiteral(red: 0.807843148708344, green: 0.0274509806185961, blue: 0.333333343267441, alpha: 1.0)
        rectangle.strokeColor = #colorLiteral(red: 0.439215689897537, green: 0.0117647061124444, blue: 0.192156866192818, alpha: 1.0)
        rectangle.lineWidth = 5
        rectangle.alpha = 0.4
        let labelNode = SKLabelNode(text: "Hello World")
        labelNode.fontSize = 20
        labelNode.fontName = "Arial"
        labelNode.position = CGPoint(x:0,y:0)
        skScene.addChild(rectangle)
        skScene.addChild(labelNode)
        
        
        let plane = SCNPlane(width: 0.20, height: 0.20)
        let material = SCNMaterial()
        material.isDoubleSided = true
        material.diffuse.contents = skScene
        plane.materials = [material]
        let planeNode = SCNNode(geometry: plane)

        return planeNode
    }
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
