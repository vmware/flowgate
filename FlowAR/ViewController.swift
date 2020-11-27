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
    
    var temperatures = [25.5, 25.8, 28, 26.9]
    
    var qrRequests = [VNRequest]()
    var detectedDataAnchor: [String: ARAnchor?] = [:] // QR location
    var message: String! // QR Message === ID
    var detectedDataResult: [String: [String: Any]] = [:] // [ID: ["AssetId":"sfsdf", "AssetName": "sfdsfd"]]
    var cabinet: String! // cabinet name -> getAssetByName
    var cabitnetNode: SCNNode?
    
    var paused = false
    
    var chartNode: SCNNode?
    var addPlane: SCNNode?
    
    /// The view controller that displays the status and "restart experience" UI.
    lazy var statusViewController: StatusViewController = {
        return children.lazy.compactMap({ $0 as? StatusViewController }).first!
    }()
    /// A serial queue for thread safety when modifying the SceneKit node graph.
    let updateQueue = DispatchQueue(label: Bundle.main.bundleIdentifier! +
        ".serialSceneKitQueue")
    
    /// Convenience accessor for the session owneASd by ARSCNView.
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
    var cabinet_show = false;
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
        statusViewController.pauseSessionHandler = {[unowned self] in
            if(paused){
                self.paused = false
                let configuration = ARWorldTrackingConfiguration()
                session.run(configuration, options: [])
                statusViewController.showPause()
            }else{
                self.paused = true
                session.pause()
                statusViewController.showContinue()
            }}
    
    }
//    override func viewWillAppear(_ animated: Bool) {
//        super.viewWillAppear(animated)
//        print("heihei")
//
//    }
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
    
    func stopFigure(){
        let configuration = ARWorldTrackingConfiguration()
        session.run(configuration)
    }
    func resetTracking() {
        
//        guard let referenceImages = ARReferenceImage.referenceImages(inGroupNamed: "AR Resources", bundle: nil) else {
//            fatalError("Missing expected asset catalog resources.")
//        }
//
        statusViewController.hidePause()
        statusViewController.showPause()
        let configuration = ARWorldTrackingConfiguration()
        session.run(configuration, options: [.resetTracking, .removeExistingAnchors])

        statusViewController.scheduleMessage("Look around to detect server", inSeconds: 7.5, messageType: .contentPlacement)
        cabinet_b = false;
        cabinet_show = false;
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
            DispatchQueue.main.async {
                self.statusViewController.cancelAllScheduledMessages()
                self.statusViewController.showMessage("Detected a bar code")
            }
            // Get the bounding box for the bar code and find the center
            var rect = result.boundingBox
            // TODO: Draw it
            // Flip coordinates
            rect = rect.applying(CGAffineTransform(scaleX: 1, y: -1))
            rect = rect.applying(CGAffineTransform(translationX: 0, y: 1))
            // Get center
            let center = CGPoint(x: rect.midX, y: rect.midY)
            if(cabinet_show)
            {
                DispatchQueue.main.async {
                self.statusViewController.cancelAllScheduledMessages()
                self.statusViewController.showMessage("Loading information")
                }
                DispatchQueue.main.async {
                self.hitTestQrCode(center: center, message: message)
                self.processing = false
                }
            }else{
                getAssetByIDNAnchor(ID: message)
                self.hitTestQrCodeFirst(center: center, message: message)
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
                node.transform = SCNMatrix4(hitTestResult.worldTransform)
                node.rotation = self.cabitnetNode?.rotation ?? node.rotation
            } else {
                // Create an anchor. The node will be created in delegate methods
//                self.detectedDataAnchor[message] = hitTestResult.anchor
                self.detectedDataAnchor[message] = ARAnchor(transform: hitTestResult.worldTransform)
                self.lastAddedAnchor = self.detectedDataAnchor[message] as? ARAnchor
                self.getAssetByID(ID: message)
            }
        }
    }
    
    func hitTestQrCodeFirst(center: CGPoint, message: String) {
//        print(detectedDataResult)
        if let hitTestResults = sceneView?.hitTest(center, types: [.featurePoint] ),
           let hitTestResult = hitTestResults.first {
                // Create an anchor. The node will be created in delegate methods
//                self.detectedDataAnchor[message] = hitTestResult.anchor
                self.detectedDataAnchor[message] = ARAnchor(transform: hitTestResult.worldTransform)
                self.lastAddedAnchor = self.detectedDataAnchor[message] as? ARAnchor
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
    
    // Anchor(定位) ->Node(Anchor) object -> node加进你要的返回的node
    // Geometry     _^

    // MARK: - ARSCNViewDelegate

    
    // Override to create and configure nodes for anchors added to the view's session.
    func renderer(_ renderer: SCNSceneRenderer, nodeFor anchor: ARAnchor) -> SCNNode? {
        if self.lastAddedAnchor?.identifier == anchor.identifier {
            
//            DispatchQueue.main.async {
//            self.statusViewController.cancelAllScheduledMessages()
//            self.statusViewController.showMessage("add this anchor")
//            }
            let node = SCNNode()
            guard let ID = self.message else { return node }
            let result = strFormat(content: detectedDataResult[ID]! as [String: Any])
            let left_message = textNode(text: result["type"]!,  position: SCNVector3(-0.11, 0.05, 0.01))
            let right_message = textNode(text: result["content"]!, position: SCNVector3(0, 0.05, 0.01))
            let title_message = textNode(text: result["title"]!, position: SCNVector3(-0.11, 0.09, 0.01), bold: true, size: 2)
            left_message.eulerAngles.x = 0
            right_message.eulerAngles.x = 0
            title_message.eulerAngles.x = 0
            
            left_message.opacity = 0
            right_message.opacity = 0
            title_message.opacity = 0
            node.addChildNode(left_message)
            node.addChildNode(right_message)
            node.addChildNode(title_message)


            let plane = SCNPlane(width: 0.25, height: 0.2)
            let planeNode = SCNNode(geometry: plane)
            planeNode.eulerAngles.x = 0
            planeNode.opacity = 0 // for fadein
            node.addChildNode(planeNode)
            
            let underLine = SCNPlane(width: 0.25, height: 0.002)
            underLine.firstMaterial?.diffuse.contents = UIColor.cyan
            let underLineNode = SCNNode(geometry: underLine)
            underLineNode.eulerAngles.x = 0
            underLineNode.opacity = 0 // for fadein
            underLineNode.position = SCNVector3Make(0, 0.06, 0.001)
            node.addChildNode(underLineNode)
            
            planeNode.scale = SCNVector3Zero
            planeNode.runAction(self.imageAppearAction)
            underLineNode.scale = SCNVector3Zero
            underLineNode.runAction(self.imageAppearAction)

            
            title_message.runAction(self.textAppearAction)
            left_message.runAction(self.textAppearAction)
            right_message.runAction(self.textAppearAction)
            
            
            
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
                node.addChildNode(wireFrame)
                self.cabitnetNode = wireFrame
                
                // add lines
                
                var start_y = -referenceImage.physicalSize.height/2
                let end_y = referenceImage.physicalSize.height/2 - (referenceImage.physicalSize.height/42) * 3
                let part = (referenceImage.physicalSize.height/42) * 3 // every 3 units
                let text_pos = part/2
                var iter = 40

                
                while(start_y<end_y){
                    let lineGeometry = SCNCylinder()
                    lineGeometry.radius = 0.002
                    lineGeometry.height  = CGFloat(referenceImage.physicalSize.width)
                    lineGeometry.radialSegmentCount = 5
                    lineGeometry.firstMaterial!.diffuse.contents = UIColor.red
                    let lineNode = SCNNode(geometry: lineGeometry)
                    lineNode.position = SCNVector3(0, 0, start_y)
                    lineNode.eulerAngles.x = -.pi/2
                    lineNode.eulerAngles.y = -.pi/2
                    node.addChildNode(lineNode)
                    
                    // display row number
                    let string = String(iter) + "-" + String(iter+2)
                    let text = SCNText(string: string, extrusionDepth: 0.1)
                    text.font = UIFont.systemFont(ofSize: 5)
                    text.flatness = 0.005
                    let textNode = SCNNode(geometry: text)
                    let fontScale: Float = 0.01
                    textNode.scale = SCNVector3(fontScale, fontScale, fontScale)
                    
                    let (min, max) = (text.boundingBox.min, text.boundingBox.max)
                    let dx = min.x + 0.5 * (max.x - min.x)
                    let dy = min.y + 0.5 * (max.y - min.y)
                    let dz = min.z + 0.5 * (max.z - min.z)
                    textNode.pivot = SCNMatrix4MakeTranslation(dx, dy, dz)
                    textNode.position = SCNVector3Make(0, 0, Float(start_y + text_pos))
                    textNode.opacity = 0.8
                    textNode.eulerAngles.x = -.pi / 2
                    node.addChildNode(textNode)
                    
                    let plane = SCNPlane(width: CGFloat(referenceImage.physicalSize.width), height: CGFloat(referenceImage.physicalSize.height/42) * 3)
                    let planeNode = SCNNode(geometry: plane)
                    planeNode.geometry?.firstMaterial?.diffuse.contents = UIColor.white.withAlphaComponent(0.5)
                    planeNode.geometry?.firstMaterial?.isDoubleSided = true
                    planeNode.position = SCNVector3Make(textNode.position.x, textNode.position.y, textNode.position.z + 0.001)
                    
                    planeNode.eulerAngles.x = -.pi / 2
                    node.addChildNode(planeNode)
                    iter -= 3 // rack number +3

                    start_y+=part
                }
                
                
                let result = self.strFormat(content: self.fetch_result as [String: Any])
                let left_message = self.textNode(text: result["type"]!,position: SCNVector3( Float(referenceImage.physicalSize.width+0.01), 0.001, -0.05))// (x,y,z: length,depth,height)
                let right_message = self.textNode(text: result["content"]!, position: SCNVector3(Float(referenceImage.physicalSize.width+0.12), 0.001, -0.05))
                let title_message = self.textNode(text: result["title"]!, position: SCNVector3(Float(referenceImage.physicalSize.width+0.01), 0.001, -0.09), bold: true, size: 2)
                
                left_message.opacity = 0
                right_message.opacity = 0
                title_message.opacity = 0
                node.addChildNode(left_message)
                node.addChildNode(right_message)
                node.addChildNode(title_message)

                let plane = SCNPlane(width: 0.25, height: 0.18)
//                plane.cornerRadius = 0.02
                let planeNode = SCNNode(geometry: plane)
                planeNode.eulerAngles.x = 0
                planeNode.opacity = 0 // for fadein
                planeNode.position = SCNVector3Make(Float(referenceImage.physicalSize.width+0.115), 0, -0.10)
                planeNode.pivot = SCNMatrix4MakeTranslation(0, 0.1, 0)
                
                node.addChildNode(planeNode)
                
                let text = self.textNode(text: "Show Temperature Plots", position: SCNVector3(Float(referenceImage.physicalSize.width+0.01), 0.002, 0.045), color: .white)
                text.opacity = 0
                text.name = "temp"
                node.addChildNode(text)
                
                let chartPlane = self.planeNode(width: 0.145, height: 0.015, opacity: 0, position: SCNVector3Make(Float(referenceImage.physicalSize.width) + 0.08, 0.001, 0.05), conor: 0.02)
                chartPlane.geometry?.firstMaterial?.diffuse.contents = UIColor.blue
                chartPlane.name = "temp"
                node.addChildNode(chartPlane)
                
                self.addPlane = self.planeNode(width: 0.25, height: 0.16, opacity: 0, position: SCNVector3Make(Float(referenceImage.physicalSize.width+0.115), -0.001, 0.07))
                planeNode.pivot = SCNMatrix4MakeTranslation(0, 0.08, 0)
                self.addPlane!.pivot = SCNMatrix4MakeTranslation(0, 0.08, 0)
                node.addChildNode(self.addPlane!)
                
                self.chartNode = self.add_chart()
                self.chartNode!.position = SCNVector3(Float(referenceImage.physicalSize.width) + 0.012, 0.001, 0.18)
                self.chartNode!.isHidden = true
                node.addChildNode(self.chartNode!)
                
                let underLine = SCNPlane(width: 0.25, height: 0.002)
                underLine.firstMaterial?.diffuse.contents = UIColor.blue
                let underLineNode = SCNNode(geometry: underLine)
                underLineNode.eulerAngles.x = 0
                underLineNode.opacity = 0 // for fadein
                underLineNode.position = SCNVector3Make(Float(referenceImage.physicalSize.width+0.115), 0.001, -0.06)
                node.addChildNode(underLineNode)
                
                // Add pointing line
                let frameNode = self.lineNode(color: .red, height: CGFloat(referenceImage.physicalSize.width), position: SCNVector3(referenceImage.physicalSize.width/2.0 - 0.25, 0, 0), angle: 0)
                node.addChildNode(frameNode)
                
                planeNode.eulerAngles.x = -.pi / 2
                underLineNode.eulerAngles.x = -.pi / 2
                
                // animation
                frameNode.scale = SCNVector3Zero
                frameNode.runAction(self.imageAppearAction)
                planeNode.scale = SCNVector3Zero
                planeNode.runAction(self.imageAppearAction)
                underLineNode.scale = SCNVector3Zero
                underLineNode.runAction(self.imageAppearAction)
                
                chartPlane.runAction(self.textAppearAction)
                
                title_message.runAction(self.textAppearAction)
                left_message.runAction(self.textAppearAction)
                right_message.runAction(self.textAppearAction)
                text.runAction(self.textAppearAction)
                self.cabinet_show = true;
                self.stopFigure()
                self.sceneView.session.add(anchor: self.lastAddedAnchor!)
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
    
    var imageAppearAction: SCNAction {
        return .group([
            .fadeOpacity(to: 0.8, duration: 0.8),
            .scale(to: 1.0, duration: 0.8),
            
        ])
    }
    var textAppearAction: SCNAction {
        return .sequence([
            .wait(duration: 0.25),
            .fadeIn(duration: 0.8),
        ])
    }
    
    func textNode(text: String, position: SCNVector3, bold: Bool=false,
                       size: Float=1, center: Bool=false, color: UIColor = .black) -> SCNNode {
        let mesages = SCNText(string: text, extrusionDepth: 0) // SCN开头的geometry
        if(bold) {mesages.font = UIFont(name:"HelveticaNeue-Bold", size: 12)}
        else {mesages.font = UIFont(name:"HelveticaNeue", size: 12)}
        let material = SCNMaterial()
        material.diffuse.contents = color
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
            messageNode.position = messageNode.position + position
        }
        else {messageNode.pivot = SCNMatrix4MakeTranslation(
            minVec.x,
            maxVec.y,
            minVec.z
        )
        messageNode.eulerAngles.x = -.pi/2
        messageNode.position = messageNode.position + position
        }
        
        return messageNode
    }
    
    func lineNode(color: UIColor, height: CGFloat, position: SCNVector3, angle: Float) -> SCNNode{
        // Add vertical line
        let LineConnect = SCNCylinder()
        LineConnect.radius = 0.001
        LineConnect.height  = height
        LineConnect.radialSegmentCount = 5
        LineConnect.firstMaterial!.diffuse.contents = color
        let LineNode = SCNNode(geometry: LineConnect)
        LineNode.pivot = SCNMatrix4MakeTranslation(
            0,
            -Float(height/2),
            0
        )
        LineNode.position = position
        LineNode.eulerAngles.x = -.pi/2
        LineNode.eulerAngles.y = -.pi/2+angle
        
        return LineNode
    }
    
    func ballNode(color: UIColor, position: SCNVector3, radius: CGFloat) -> SCNNode{
        let ballGeo = SCNSphere()
        ballGeo.radius = radius
        ballGeo.firstMaterial!.diffuse.contents = color
        let ballNode = SCNNode(geometry: ballGeo)
        ballNode.position = position
        return ballNode
    }
    
    func planeNode(width: CGFloat, height: CGFloat, opacity: CGFloat, position: SCNVector3, conor: CGFloat = 0)->SCNNode{
        let plane = SCNPlane(width: width, height: height)
        plane.cornerRadius = conor
        let planeNode = SCNNode(geometry: plane)
        planeNode.eulerAngles.x = -.pi/2
        planeNode.opacity = opacity
        planeNode.position = position
        return planeNode
    }
    
    func see_chart_button() -> SCNNode{
        let node = SCNNode()
        node.addChildNode(textNode(text: "Show Temperature Plots", position: SCNVector3(0, 0, 0), color: .brown))
        node.addChildNode(lineNode(color: .cyan, height: 0.1/cos(.pi/2), position: SCNVector3(0, 0, 0), angle: -.pi/4))
        node.addChildNode(lineNode(color: .cyan, height: 0.1/cos(.pi/2), position: SCNVector3(0, 0, 0), angle: .pi/4))
        return node
    }

    
    @IBAction func tap_add_chart(_ sender: UITapGestureRecognizer) {
        let currentTouchLocation = sender.location(in: sceneView)
        print(currentTouchLocation)
        guard let hitTestResultNode = self.sceneView.hitTest(currentTouchLocation, options: nil).first?.node else { return }
        guard let name = hitTestResultNode.name else{ return }
        print("name")
        if(name=="temp"){
            // TODO
            print("here")
            self.chartNode?.isHidden = false
            self.addPlane?.runAction(self.imageAppearAction)
        }
        
    }
    
    func add_chart() -> SCNNode{
        let date = Date()
        let calendar = Calendar.current
        let hour = calendar.component(.hour, from: date)
        let minutes = calendar.component(.minute, from: date)
        let node = SCNNode()
        node.addChildNode(lineNode(color: .darkGray, height: 0.1, position: SCNVector3(0, 0, 0), angle: .pi/2))
        node.addChildNode(lineNode(color: .darkGray, height: 0.2, position: SCNVector3(0, 0, 0), angle: 0))
        let height = temperatures.max()! - temperatures.min()!
        let length = 0.18/Double(temperatures.count-1)
        var now_height = 0.005
        for i in 0...(temperatures.count-2){
            let height = (temperatures[i+1] - temperatures[i])/height * 0.09
            let angle = atan(height/length)
            node.addChildNode(lineNode(color: .cyan, height: CGFloat(length/cos(angle)), position: SCNVector3(0.01 + length * Double(i), 0, -now_height), angle: Float(angle)))
            node.addChildNode(ballNode(color: .cyan, position: SCNVector3(0.01 + length * Double(i), 0, -now_height), radius: 0.001))
            node.addChildNode(textNode(text: String(temperatures[i]), position: SCNVector3Make(Float(0.01 + length * Double(i)), 0, -Float(now_height)), bold: false, size: 1, color: .darkGray))
            node.addChildNode(textNode(text: String(hour) + ":" + String(minutes - temperatures.count + 1 + i), position: SCNVector3(Float(0.01 + length * Double(i)), 0, 0.01), bold: false, size: 1, color: .darkGray))
            now_height += height
        }
        let i = temperatures.count - 1
        node.addChildNode(textNode(text: String(temperatures[i]), position: SCNVector3(Float(0.01 + length * Double(i)), 0, -Float(now_height)), bold: false, size: 1, color: .darkGray))
        node.addChildNode(textNode(text: String(hour) + ":" + String(minutes), position: SCNVector3(Float(0.01 + length * Double(i)), 0, 0.01), bold: false, size: 1, color: .darkGray))
        node.addChildNode(textNode(text: "time: ", position: SCNVector3(-0.02, 0, 0.01), bold: false, size: 1, color: .darkGray))
        return node
    }
    
}

extension SCNVector3 {
    static func + (left: SCNVector3, right: SCNVector3) -> SCNVector3 {
        return SCNVector3Make(left.x + right.x, left.y + right.y, left.z + right.z)
    }
}
