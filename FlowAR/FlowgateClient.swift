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
    
    func getAssetByName(name: String) {
        let _token = self.getFlowgateToken()
        let config = URLSessionConfiguration.default
        let session = URLSession(configuration: config, delegate: self, delegateQueue: OperationQueue.main)
        let token_url = URL(string: self.host + "/apiservice/v1/assets/name/" + name + "/")
        var request = URLRequest(url: token_url!)
        // header
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        guard let acc_token = _token["access_token"] as? String else {
            return
        }
        print(acc_token)
        request.addValue(("Bearer " + acc_token), forHTTPHeaderField: "Authorization")
        request.httpMethod = "GET"
        print("getAssetByName")
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
                        self.cabinet_b = true
                    }catch _{ print("JSONSerialization error:", error as Any)}
                }
            }
    })
        task.resume()
    }
    
    func getAssetByID(ID:String){
//        DispatchQueue.main.async {
//        self.statusViewController.cancelAllScheduledMessages()
//        self.statusViewController.showMessage("getAssetByID")
//        }
        print("getAssetById")
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
//                    DispatchQueue.main.async {
//                        self.statusViewController.cancelAllScheduledMessages()
//                        self.statusViewController.showMessage("Internet")
//                    }
                        let info = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any]
                        self.detectedDataResult[ID] = info
                        print("getAssetByIdonline")
                        self.sceneView.session.add(anchor: self.detectedDataAnchor[ID]!!)
                        
                    }catch _{ print("JSONSerialization error:", error as Any)}
                }
            }
        })
        task.resume()
    }

    func getAssetByIDNAnchor(ID: String){

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
                        let info = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any]
                        self.detectedDataResult[ID] = info
//                        self.lastAddedAnchor = self.detectedDataAnchor[ID] as? ARAnchor
                        
                            self.statusViewController.showPause()
                            self.statusViewController.unhidePause()
                            self.cabinet = info?["cabinetName"] as? String
                            DispatchQueue.main.async {
                                guard let _ = self.cabinet else {
                                    return
                                }}
                        DispatchQueue.main.async {
                                self.statusViewController.cancelAllScheduledMessages()
                                self.statusViewController.showMessage("Work around to detect the rack" + self.cabinet)
                            }
                            guard let _ = self.cabinet else {
                                self.statusViewController.cancelAllScheduledMessages()
                                self.statusViewController.showMessage("Not in cabinet")
                                return
                            }
                        print("getAssetByIDNAnchor")
                            self.getAssetByName(name: self.cabinet)
                        
                    }catch _{ print("JSONSerialization error:", error as Any)}
                }
            }
        })
        task.resume()
    }
    
    func strFormat(content: [String: Any]) -> [String: String]{
//        let content = self.detectedDataResult[ID]! as [String: Any]
        var result: [String: String] = ["type":"", "content":"", "title":""]
        for item in items{
            if item == "assetName" {
                result["title"] = content[item] as? String
                continue
            }
            if item.contains("."){
                let item_l = item.split(separator: ".") // "xxx.aa/bb" get [xx, aa/bb]
                let left = String(item_l[0]) // left = xx
                guard let temp_o = content[left] as? [String: Any] else {return ["error":"no a list"]} // temp_o = content[xx]
                let item_i = item_l[1].split(separator: "/") // [aa, bb]
                result["type"]! += left + "\n"
                result["content"]! += "\n"
                for item_in in item_i{ // aa
                    result["type"]! += "\t" + item_in + "\n"
                    if let temp = temp_o[String(item_in)] as? String {
                        result["content"]! += temp + "\n"
                    } else if let temp = temp_o[String(item_in)] as? Int {
                        result["content"]! += String(format: "%d", temp) + "\n"
                    }
                }
            }
            else {
                if let temp = content[item] as? String {
                    result["type"]! += item + "\n"
                    result["content"]! += temp + "\n"
                } else if let temp = content[item] as? Int {
                    result["type"]! += item + "\n"
                    result["content"]! += String(format: "%d", temp) + "\n"
                }
            }
        }
        return result
    }
    
    
}
