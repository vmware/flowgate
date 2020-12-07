/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { CommonModule } from '@angular/common';
import {Component, ElementRef ,ViewChild, NgModule, CUSTOM_ELEMENTS_SCHEMA, AfterViewInit, OnInit} from '@angular/core'
import { SddcsoftwareModule } from 'app/pages/sddcsoftware/sddcsoftware.module';
import { AssetModule } from 'app/pages/setting/component/asset-modules/asset.module';
import { ServerMappingDataModule } from 'app/pages/servermapping/mapping.module';
import { SettingService } from '../../setting.service';
import { MetricJsonData, Node, Link , LinkMap} from './sankey.data.service';

declare var d3;

@Component({
    selector: 'setting-asset',
    template: '<div class="clr-row" style="margin-top: 22px;">\
                <div class="col-lg-2 col-sm-2 col-2" style="text-align: center;">\
                    <label>Select VMware vCenter</label>\
                </div>\
                <div class="clr-col-4">\
                    <div class="form-group">\
                        <div class="clr-select-wrapper">\
                            <select class="clr-select" id="select" on-change="change($event)" > \
                                <option value="">Please Select</option>\
                                <option *ngFor="let vcenter of allVcenter" value="{{vcenter.id}}" >{{vcenter.name}}</option>\
                            </select>\
                        </div>\
                    </div>\
                </div>\
                <div class="col-lg-6 col-sm-6 col-6">\
                </div>\
                </div>\
                <br\>\
                <br\>\
                <div class="row father_width" #target>\
                </div>\
                <div id="Legend">\
                    <span class="label label-info" style="border:0px; color:white;background:#006a91;width: 75px;">vCenter</span>\
                    <span class="label label-success" style="border:0px; color:white;background:#bbcdd6;width: 75px;">Host</span>\
                    <span class="label label-warning" style="border:0px; color:white;background:#ea924c;width: 75px;">PDU</span>\
                    <span class="label label-danger" style="border:0px; color:white;background:#04273e;width: 75px;">Switch</span>\
                </div>'
})

export class AssetChart implements AfterViewInit, OnInit{
    
    @ViewChild('target') target: ElementRef;

    constructor(private service:SettingService) {
       
    }

    allVcenter:SddcsoftwareModule[] = [];

    setPduSwitchData(data:AssetModule, num, hostIndex, type, nodes:Node [], links:Link []){

        let flag:Boolean = false;
        
        let index:number = 0;

        nodes.forEach((e) =>{
            if(e.asset == data.id){//if same
                flag = true;
                index = e.index;
            }
        })
        if(flag == false){
            let node = new Node(data.assetName, type, data.id, num, []);
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
            (data:ServerMappingDataModule[])=>{
                    data.forEach(e => {
                        if(e.asset != null){
                            howManyAsset++;
                        }
                    })

                    data.forEach(e => {
                        if(e.asset == null){
                            let node = new Node(e.vcHostName, "host", e.asset, num, []);
                            nodes.push(node);
                            let link = new Link(0, num, 1);
                            links.push(link);
                            num++;
                        }else{
                            this.service.getAssetById(e.asset).subscribe(// use vc's assetid search host
                                (data:AssetModule)=>{
                                    let linkMaps = [];
                                    if(data.justificationfields['DEVICE_PORT_FOR_SERVER']){
                                        let maps: string = data.justificationfields['DEVICE_PORT_FOR_SERVER'];
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
                                    pdus = data.pdus;
                                    switches = data.switches;
                                    
                                    if(pdus != null){
                                        pdus.forEach(e => {
                                            this.service.getAssetById(e).subscribe(//use host's pdu search asset for get pdu info
                                                (data:AssetModule)=>{
                                                    num = this.setPduSwitchData(data, num, hostIndex, "pdu", nodes, links);
                                                    pduTime++;
                                                }
                                            )
                                        })
                                    }
                                    if(switches != null){
                                        switches.forEach(e => {
                                            this.service.getAssetById(e).subscribe(
                                                (data:AssetModule)=>{
                                                    num = this.setPduSwitchData(data, num, hostIndex, "switch", nodes, links);
                                                    switchTime++;
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
        )
    }

    
    render(jsonString) {
        if(jsonString.links.length == 0){
            return;
        }
        /*sort nodes*/
        let nodes_new = [];

        jsonString.nodes.forEach((e, i) => {
            if((e.id == "vcenter" || e.id == "host") && e.asset != null){
                nodes_new.push(e);
            }
        });

        for(var i=0; i < jsonString.nodes.length; i++){
            var flag = 0;
            for(var j=0; j < nodes_new.length; j++){
                if(nodes_new[j].index == jsonString.nodes[i].index){
                    flag = 1;
                }
            }
            if(flag == 0){
                nodes_new.push(jsonString.nodes[i]);
            }
        }

        nodes_new.forEach((node, i) => {
            if((parseInt(node.index) != i) && (node.id == "host") && (node.asset != null)){
                jsonString.links.forEach((link,j) => {
                    if(link.source == node.index){
                        link.source = i;
                    }
                });
            }
        });
        
        let jsonString_new:any;
        jsonString_new = new MetricJsonData(nodes_new, jsonString.links);

        let father_width = <HTMLDivElement>document.querySelector(".father_width");
        let svg = d3.select(this.target.nativeElement).append("svg").attr("width", father_width.clientWidth-100)
        .attr("height", 74*jsonString_new.nodes.length);

        let chart = svg.chart("Sankey.Path");
        
        chart.on('node:click', function(node) {
            alert('Clicked on ' + node.name);
        })
        .colorLinks('#00bbff')
        .nodeWidth(20)
        .nodePadding(50)
        .spread(true)
        .draw(jsonString_new);

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
        jsonString_new.links.forEach((e, i) => {
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
        let legend = <HTMLDivElement>document.querySelector("#Legend");
        if(jsonString_new.nodes.length > 10){
            legend.setAttribute('style', 'margin-top: -'+1.3*jsonString_new.nodes.length+'%');
        }
        
    }

    getVcenterSelect(){
        this.service.getAllVcenter().subscribe(
            (data:SddcsoftwareModule[])=>{
                this.allVcenter = data;
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
