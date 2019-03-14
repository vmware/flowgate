/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
export class Logincache {

    public userName:string;
    public password:string;
    public id:string;

    public setUserName(userName:string):void {
        this.userName = userName;
    }

    public getUserName():string {
        return this.userName;
    }
   
    
    public setPassword(password:string):void {
        this.password = password;
    }

    public getPassword():string {
        return this.password;
    }

    public setId(id:string):void {
        this.id = id;
    }

    public getId():string {
        return this.id;
    }
}
