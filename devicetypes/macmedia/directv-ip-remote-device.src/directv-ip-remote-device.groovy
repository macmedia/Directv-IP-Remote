/**
*  Directv IP Remote - Device Type
*  Version 1.0.0 - 05/04/2015
*
*  Using the Directv IP Tuner Smart App will create a new virtual switch that can be used
*  by Amazon Echo to say voice cammands to turn the channel.
*
*  Copyright 2016 Mike Elser
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*/
metadata {
    definition (name: "Directv IP Remote Device", namespace: "macmedia", author: "Mike Elser") {
        capability "Switch"
        capability "Momentary"
        capability "Actuator"
        capability "refresh"
    }

    command "hubGet"
    command "setVal"

    attribute "channel","String"

    simulator {
        // TODO: define status and reply messages here
    }

    tiles (scale:2){
        standardTile("switch", "device.switch", width: 2, height: 2, decoration: "flat", canChangeIcon: false, canChangeBackground: false) {
            state "off", label: 'Push', action: "momentary.push", icon: "st.Electronics.electronics18", backgroundColor: "#FFFFFF"
            state "on",  label: 'Push', action: "momentary.push", icon: "st.Electronics.electronics18", backgroundColor: "#53A7C0"
        }

        valueTile("channel", "device.channel", decoration: "flat",width: 4, height: 2) {
            state("default", label:'Channel ${currentValue}')
        }


        //htmlTile(name:"devInfoHtml", action: "getInfoHtml", width: 4, height: 2)

        main "switch"
        details(["switch", "channel"])
    }
}

//HTML TILE MAPPING
mappings { path("/getInfoHtml") {action: [GET: "getInfoHtml"]} }


def setVal(){
	state.channelLabel = "3"
	sendEvent(name: "channel", value: "3", isStateChange: true)
}


def initialize(){
	log.debug("init")
}

def push() {
    sendEvent(name: "switch", value: "on", isStateChange: true, display: false)
    sendEvent(name: "switch", value: "off", isStateChange: true, display: false)
    sendEvent(name: "momentary", value: "pushed", isStateChange: true)

    hubGet()
}

// parse events into attributes
def parse(String description) {
    log.debug "Parsing '${description}'"
    // TODO: handle 'switch' attribute

}

// handle commands
def on() {
    push()
}

def off() {
    push()
}

def getInfoHtml(){
	def channel = parent.channel
    renderHTML {
    	head{"""
        <style type="text/css">
        .htmlTile{
			font-size: 14px;
        }
        .number{}
        </style>
        """}

        body{"""

        <div class="htmlTile">Tune to Channel <span class="number">$channel</span></div>
        """}
    }
}

private hubGet(){

    def address = convertHexToIP(parent.state.dtvip)+":8080"
    def channel = parent?.channel
    def url = "/tv/tune?major=$channel"
    def hubAction = new physicalgraph.device.HubAction(
        method: "GET",
        path: url,
        headers: [HOST:address]
    )

    return hubAction
}

def String convertHexToIP(hex){
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

def Integer convertHexToInt(hex){
	Integer.parseInt(hex,16)
}