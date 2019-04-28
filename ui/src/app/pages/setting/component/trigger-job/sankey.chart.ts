/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import {Component, ElementRef ,ViewChild, NgModule, CUSTOM_ELEMENTS_SCHEMA, AfterViewInit, OnInit} from '@angular/core'
import { SettingService } from '../../setting.service';
import { MetricJsonData, Node, Link , LinkMap} from './sankey.data.service';

declare var d3;

@NgModule({
    schemas: [ CUSTOM_ELEMENTS_SCHEMA  ]
})
@Component({
    selector: 'setting-asset',
    template: '<div class="row" style="margin-top: 22px;">\
                <div class="col-lg-2 col-sm-2 col-2" style="text-align: center;">\
                    <label>Select vCenter</label>\
                </div>\
                <div class="col-lg-4 col-sm-4 col-4">\
                    <div class="form-group">\
                        <div class="select">\
                            <select id="select" on-change="change($event)" > \
                                <option value="">Please Select</option>\
                                <option *ngFor="let vcenter of allVcenter" value="{{vcenter.id}}" >{{vcenter.name}}</option>\
                            </select>\
                        </div>\
                    </div>\
                </div>\
                <div class="col-lg-6 col-sm-6 col-6">\
                </div>\
                </div>\
                <div class="row">\
                    <h1>\
                        <svg #target width="1500" height="700"></svg>\
                    </h1>\
                </div>'
})


export class AssetChart implements AfterViewInit, OnInit{
    
    @ViewChild('target') target: ElementRef;

    constructor(private service:SettingService) {
       
    }

    allVcenter = [{
        id: "",
        name: "",
        description: "",
        userName: "",
        password: "",
        integrationStatus: "",
        serverURL:"",
        type: "",
        verifyCert: ""
    }];

    setPduSwitchData(data, num, hostIndex, type, nodes:Node [], links:Link []){

        let flag:Boolean = false;
        
        let index:number = 0;

        nodes.forEach((e) =>{
            if(e.asset == data.json().id){//if same
                flag = true;
                index = e.index;
            }
        })
        if(flag == false){
            let node = new Node(data.json().assetName, type, data.json().id, num, []);
            nodes.push(node);
            index = num;
            num++;
        }
        let link = new Link(hostIndex, index, 1);
        links.push(link);

        return num;
    }

    change(event:any){
        this.destroy();
        if(event.target.value == ""){
            return;
        }

        let jsonString:MetricJsonData;
        let nodes = [];
        let links = [];
        let num = 0;
        let queryAssetTime = 0;
        let howManyAsset = 0;
        let vcId = event.target.value;
        let vcName = event.target[event.target.selectedIndex].innerHTML;

        let node = new Node(vcName, "vcenter", vcId, num, []);
        nodes.push(node);
        num++;

        this.service.getVcenterById(vcId).subscribe(
            (data)=>{
                if(data.status == 200){
                    if(data['_body'] == ""){
                        return;
                    }

                    data.json().forEach(e => {
                        if(e.asset != null){
                            howManyAsset++;
                        }
                    })

                    data.json().forEach(e => {

                        if(e.asset == null){
                            let node = new Node(e.vcHostName, "host", e.asset, num, []);
                            nodes.push(node);
                            let link = new Link(0, num, 1);
                            links.push(link);
                            num++;
                        }else{
                            this.service.getAssetById(e.asset).subscribe(// use vc's assetid search host
                                (data)=>{
                                    if(data.status == 200){
                                        if(data['_body'] == ""){
                                            return;
                                        }
                                        
                                        let linkMaps = [];
                                        if(data.json().justificationfields['DEVICE_PORT_FOR_SERVER']){
                                            let maps: string = data.json().justificationfields['DEVICE_PORT_FOR_SERVER'];
                                            maps.split(",").forEach(e => {
                                                let map = e.split("_FIELDSPLIT_");
                                                let linkMap = new LinkMap(parseInt(map[0]), parseInt(map[2]), map[3]);
                                                linkMaps.push(linkMap);
                                            });
                                        }

                                        let node = new Node(e.vcHostName, "host", e.asset, num, linkMaps);
                                        nodes.push(node);
                                        let link = new Link(0, num, 1);
                                        links.push(link);
                                        
                                        let hostIndex = num;
                                        num++;

                                        let pdus = [];
                                        let switches = [];
                                        let pduTime = 0;
                                        let switchTime = 0;
                                        pdus = data.json().pdus;
                                        switches = data.json().switches;
                                        
                                        if(pdus != null){
                                            pdus.forEach(e => {
                                                this.service.getAssetById(e).subscribe(//use host's pdu search asset for get pdu info
                                                    (data)=>{
                                                        if(data.status == 200){
                                                            if(data['_body'] == ""){
                                                                return;
                                                            }
                                                            num = this.setPduSwitchData(data, num, hostIndex, "pdu", nodes, links);
                                                            pduTime++;
                                                        }
                                                    }
                                                )
                                            })
                                        }
                                        if(switches != null){
                                            switches.forEach(e => {
                                                this.service.getAssetById(e).subscribe(
                                                    (data)=>{
                                                        if(data.status == 200){
                                                            if(data['_body'] == ""){
                                                                return;
                                                            }
                                                            num = this.setPduSwitchData(data, num, hostIndex, "switch", nodes, links);
                                                            switchTime++;
                                                        }
                                                    }
                                                )
                                            })
                                        }
                                        let pduFinishFlag = false;
                                        let switchFinishFlag = false;
                                        let timeOut = 0;
                                        let interval = setInterval(() => {
                                            if((pdus != null && pdus.length == pduTime) || (pdus == null)){
                                                pduFinishFlag = true;
                                            }
                                            if((switches != null && switches.length == switchTime) || (switches == null)){
                                                switchFinishFlag = true;
                                            }
                                            if(pduFinishFlag && switchFinishFlag){
                                                queryAssetTime++;
                                                clearInterval(interval);
                                            }
                                            if(++timeOut == 60){
                                                clearInterval(interval);
                                            }
                                        }, 500)
                                    }
                                }
                            )
                        }
                    });
                    let timeOut = 0;
                    let interval = setInterval(() => {
                        if(queryAssetTime == howManyAsset){
                            jsonString = new MetricJsonData(nodes, links);
                            this.render(jsonString);
                            clearInterval(interval);
                        }
                        if(++timeOut == 60){
                            clearInterval(interval);
                        }
                    }, 500)
                }
            }
        )
    }

    
    render(jsonString) {
        if(jsonString.links.length == 0){
            return;
        }
        let svg = d3.select(this.target.nativeElement).append("svg");

        let chart = svg.chart("Sankey.Path");
        
        chart.on('node:click', function(node) {
            alert('Clicked on ' + node.name);
        })
        .colorLinks('#00bbff')
        .nodeWidth(20)
        .nodePadding(50)
        .spread(true)
        .draw(jsonString);

        this.target.nativeElement.querySelectorAll('.node').forEach((e:any) =>{
            let nodeColor:any;

            switch (e.dataset.nodeId) {
                case 'host':
                    nodeColor = '#bbcdd6';
                    break;
                case 'pdu':
                    nodeColor = '#ea924c';
                    break;
                case 'switch':
                    nodeColor = '#04273e';
                    break;
                case 'vcenter':
                    nodeColor = '#006a91';
                    break;
                default:
                    nodeColor = '#006a91';
                    break;
            }
            e.querySelector('rect').style.fill = nodeColor;

        })
        
        let allDom: string;
        jsonString.links.forEach((e, i) => {
            if(e.source.id == "host"){
                if(e.source.link){
                    e.source.link.forEach(ele => {
                        if(e.target.asset == ele.asset){
                            let dom : string = '<text dominant-baseline="middle" font-size="14">\
                                            <textPath xlink:href="#'+'link_' + e.source.index + '_' +e.target.index+'" startOffset="0%">\
                                                <tspan>'+'startPort:' + ele.startport + '/' +'endPort:' + ele.endport+'</tspan>\
                                            </textPath>\
                                        </text>';
                            allDom += dom;
                        }
                    })
                }
            }
        })
        this.target.nativeElement.querySelector('svg').innerHTML += '<g class="linkmaps">'+allDom+'</g>';
        
    }

    getVcenterSelect(){
        this.service.getAllVcenter().subscribe(
            (data)=>{
                if(data.status == 200){
                    this.allVcenter = data.json();
                }
            }
        )
    }

    ngOnInit(){
        this.getVcenterSelect();
    }
    ngAfterViewInit(){

    }
    destroy() {
        d3.select(this.target.nativeElement).selectAll('svg').remove();
    }
}