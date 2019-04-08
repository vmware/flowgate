/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
export class MetricJsonData{
    nodes: Node[];
    links: Link[];

    constructor(nodes: Node[], links: Link[]){
        this.nodes = nodes;
        this.links = links;
    }
}

export class Node{
    name: string;
    id: string;
    asset: string;
    index: number;
    link: LinkMap[];

    constructor(name: string, id : string, asset : string, index : number, link : LinkMap[]){
        this.name = name;
        this.id = id;
        this.asset = asset;
        this.index = index;
        this.link = link;
    }
}

export class Link{
    source: number;
    target: number;
    value: number;

    constructor(source: number, target: number, value: number){
        this.source = source;
        this.target = target;
        this.value = value;
    }
}

export class LinkMap{
    startport: number;
    endport: number;
    asset: string;

    constructor(startport: number, endport: number, asset: string){
        this.startport = startport;
        this.endport = endport;
        this.asset = asset;
    }
}