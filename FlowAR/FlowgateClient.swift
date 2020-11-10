//
//  FlowgateClient.swift
//  FlowAR
//
//  Created by 周舒意 on 2020/11/8.
//  Copyright © 2020 周舒意. All rights reserved.
//

import Foundation
import UIKit
import ARKit
extension ViewController{
    
    func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge, completionHandler: (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        completionHandler(URLSession.AuthChallengeDisposition.useCredential, URLCredential(trust: challenge.protectionSpace.serverTrust!))

    }
    
    
    func getFlowgateToken() -> [String: Any]{
        if (!self.current_token.isEmpty){
            let current_time = Int(round(Date().timeIntervalSince1970 * 1000))
            guard let expire_time = self.current_token["expires_in"]! as? Int else {return ["fail token":0]}
            if (expire_time - current_time > 600000){
                return self.current_token
            }
        }
        
        let config = URLSessionConfiguration.default
        let session = URLSession(configuration: config, delegate: self, delegateQueue: OperationQueue.main)
        let token_url = URL(string: self.host + "/apiservice/v1/auth/token")
        var request = URLRequest(url: token_url!)
        // header
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        // body dump to json
        if let theJSONData = try? JSONSerialization.data(
            withJSONObject: ["userName": self.username, "password": self.password],
            options: []) {
        request.httpBody = theJSONData
        request.httpMethod = "POST"
            let task = session.dataTask(with: request, completionHandler:  {(data, response, error) in
                if (error != nil){print(error.debugDescription)}
                guard let data = data, error == nil else {
                    print(error?.localizedDescription ?? "get nothing")
                    return
                }
                if let httpResponse = response as? HTTPURLResponse{
                    if httpResponse.statusCode==200{
                        do {
                            self.current_token = try JSONSerialization.jsonObject(with: data, options: []) as! [String: Any]
                        }catch _ {
                            print("JSONSerialization error:", error as Any)
                        }
                    }
                }
        })
            task.resume()
        }
        return current_token
    }
    
    func getAssetByName(name: String) -> [String: Any]{
        let _token = self.getFlowgateToken()
        let config = URLSessionConfiguration.default
        let session = URLSession(configuration: config, delegate: self, delegateQueue: OperationQueue.main)
        let token_url = URL(string: self.host + "/apiservice/v1/assets/name/" + name + "/")
        var request = URLRequest(url: token_url!)
        semaphore.wait()
        // header
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        guard let acc_token = _token["access_token"] as? String else {
            return ["There is no token":0]
        }
        print(acc_token)
        request.addValue(("Bearer " + acc_token), forHTTPHeaderField: "Authorization")
        request.httpMethod = "GET"
        let task = session.dataTask(with: request, completionHandler:  {(data, response, error) in
            if (error != nil){print(error.debugDescription)}
            print(2)
            guard let data = data, error == nil else {
                print(error?.localizedDescription ?? "get nothing")
                return
            }
            if let httpResponse = response as? HTTPURLResponse{
                if httpResponse.statusCode==200{
                    do{
                        self.fetch_result = try JSONSerialization.jsonObject(with: data, options: []) as! [String: Any]
                    }catch _{ print("JSONSerialization error:", error as Any)}
                }
            }
    })
        task.resume()
        return self.fetch_result
    }

    func getAssetByID(ID: String){

        let _token = self.getFlowgateToken()
        let config = URLSessionConfiguration.default
        let session = URLSession(configuration: config, delegate: self, delegateQueue: OperationQueue.main)
        let token_url = URL(string: self.host + "/apiservice/v1/assets/" + ID + "/")
        var request = URLRequest(url: token_url!)
        // header
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        guard let acc_token = _token["access_token"] as? String else {
            return
        }
        request.addValue(("Bearer " + acc_token), forHTTPHeaderField: "Authorization")
        request.httpMethod = "GET"
        let task = session.dataTask(with: request, completionHandler:  {(data, response, error) in
            if (error != nil){print(error.debugDescription)}
            print(2)
            guard let data = data, error == nil else {
                print(error?.localizedDescription ?? "get nothing")
                return
            }
            if let httpResponse = response as? HTTPURLResponse{
                if httpResponse.statusCode==200{
                    do{
                        self.detectedDataResult[ID] = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any]
//                        self.lastAddedAnchor = self.detectedDataAnchor[ID] as? ARAnchor
                        self.sceneView.session.add(anchor: self.detectedDataAnchor[ID]!!)
                        
                    }catch _{ print("JSONSerialization error:", error as Any)}
                }
            }
        })
        task.resume()
    }
    
    func strFormat(ID: String) -> String{
        let content = self.detectedDataResult[ID]! as [String: Any]
        var result: String = "";
        for item in items{
            guard let temp = content[item] as? String else {
                return "no this field"
            }
            result = result + item + ": " + temp + "\n"
        }
        return result
    }
}
