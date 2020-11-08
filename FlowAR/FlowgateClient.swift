//
//  FlowgateClient.swift
//  FlowAR
//
//  Created by 周舒意 on 2020/11/8.
//  Copyright © 2020 周舒意. All rights reserved.
//

import Foundation

class FlowgateClient{
    var host = "https://202.121.180.32/"      // FLOWGATE_HOST
    var password = "QWxv_3arJ70gl"         // FLOWGATE_PASSWORD
    var username = "API"
    var current_token: [String: Any] = [:]
    
    func convertToDictionary(text: String) -> [String: Any]? {
        if let data = text.data(using: .utf8) {
            do {
                return try JSONSerialization.jsonObject(with: data, options: []) as? [String: String]
            } catch {
                print(error.localizedDescription)
            }
        }
        return nil
    }
    
    func getFlowgateToken() -> [String: Any]{
        if (!self.current_token.isEmpty){
            let current_time = Int(round(Date().timeIntervalSince1970 * 1000))
            guard let expire_time = self.current_token["expires_in"]! as? Int else {}
            if (expire_time - current_time > 600000){
                return self.current_token
            }
        }

        // I didn't add verify is false
        let session = URLSession.shared
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
            _ = session.dataTask(with: request, completionHandler:  {(data, response, error) in
                guard let data = data, error == nil else {
                    print(error?.localizedDescription ?? "get nothing")
                    return
                }
                if let httpResponse = response as? HTTPURLResponse{
                    if httpResponse.statusCode==20{
                        do {
                            self.current_token = try JSONSerialization.jsonObject(with: data, options: []) as! [String: Any]
                        }catch _ {
                            print("JSONSerialization error:", error as Any)
                        }
                    }

                }

        })
        }
        return current_token
    }
}
