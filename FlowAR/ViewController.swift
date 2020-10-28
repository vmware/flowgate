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
import CoreMotion

class ViewController: UIViewController, ARSCNViewDelegate,ARSessionDelegate {

    @IBOutlet var sceneView: ARSCNView!
    
    var qrRequests = [VNRequest]()
    var detectedDataAnchor: [String: ARAnchor?] = [:]
    var lastAddedAnchor: ARAnchor?
    var processing = false
    // to know the angle
    let manager = CMMotionManager()
    var message: String!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Set the view's delegate
        sceneView.delegate = self
        sceneView.session.delegate=self
        sceneView.showsStatistics = true
        sceneView.debugOptions = [ARSCNDebugOptions.showWorldOrigin,
                                  ARSCNDebugOptions.showFeaturePoints]
     
        startQrCodeDetection()
        
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
            print(self.message ?? "No message.")
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
        if let hitTestResults = sceneView?.hitTest(center, types: [.featurePoint] ),
            let hitTestResult = hitTestResults.first {
            if let detectedDataAnchor = self.detectedDataAnchor[message],
               let node = self.sceneView.node(for: detectedDataAnchor!) {
                _ = node.position
                node.transform = SCNMatrix4(hitTestResult.worldTransform)
            } else {
                // Create an anchor. The node will be created in delegate methods
                self.detectedDataAnchor[message] = ARAnchor(transform: hitTestResult.worldTransform)
                self.lastAddedAnchor = self.detectedDataAnchor[message] as? ARAnchor
                self.sceneView.session.add(anchor: self.detectedDataAnchor[message]!!)
            }
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        // Create a session configuration
        let configuration = ARWorldTrackingConfiguration()

        // Run the view's session
        sceneView.session.run(configuration)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)

        // Pause the view's session
        sceneView.session.pause()
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

    // MARK: - ARSCNViewDelegate

    // Override to create and configure nodes for anchors added to the view's session.
    func renderer(_ renderer: SCNSceneRenderer, nodeFor anchor: ARAnchor) -> SCNNode? {

        
        if self.lastAddedAnchor?.identifier == anchor.identifier {
            
            let node = SCNNode()
            
            let mesages = SCNText(string: self.message, extrusionDepth: 0)
            let material = SCNMaterial()
            material.diffuse.contents = UIColor.black
            mesages.materials = [material]
            
            let messageNode = SCNNode(geometry: mesages)
            messageNode.scale = SCNVector3Make( 0.001, 0.001, 0.001)
            messageNode.position = messageNode.position + SCNVector3(-0.08, 0.08, 0.01)
            node.addChildNode(messageNode)
            
            let plane = SCNPlane(width: 0.2, height: 0.2)
            plane.cornerRadius = 0.02
            let planeNode = SCNNode(geometry: plane)
            planeNode.eulerAngles.x = 0
            planeNode.opacity = 0.4
            node.addChildNode(planeNode)
            
           
//            node.addChildNode(addView())
            return node
        
        }
        
        
        
        return nil
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
}

extension SCNVector3 {
    static func + (left: SCNVector3, right: SCNVector3) -> SCNVector3 {
        return SCNVector3Make(left.x + right.x, left.y + right.y, left.z + right.z)
    }
}
