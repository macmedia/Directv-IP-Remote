/**
 *  Directv IP Tuner Child
 *  Version 1.0.0 - 05/04/2016
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

definition(
    name: "Directv IP Tuner Child",
    namespace: "macmedia",
    author: "Mike Elser",
    description: "Using the Directv IP Tuner Smart App will create a new virtual switch that can be used\r\nby Amazon Echo to say voice commands to turn to the select channel.\r\n\r\n(Sample: Alexa, turn on TV NBC)\r\nChannel Name: NBC  // Is used as phrase for Alexa\r\nChannel Number: 12  // Is used as channel to tune to",
    category: "Convenience",
    parent: "macmedia:Directv IP Tuner (Connect)",
    iconUrl: "https://raw.githubusercontent.com/macmedia/Directv-IP-Tuner/master/Icons/DIRECTV.png",
    iconX2Url: "https://raw.githubusercontent.com/macmedia/Directv-IP-Tuner/master/Icons/DIRECTV@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/macmedia/Directv-IP-Tuner/master/Icons/DIRECTV@2x.png")


preferences {
    page(name:"mainPage", title:"Install Virtual Devices", nextPage: "channelPage", uninstall: true){
        section("Receiver"){
            input("recip", "text", title: "IP Address", description: "Please enter your Directv's IP address.", required:true, defaultValue:"192.168.1.100")
            input("recport", "text", title: "Port", description: "Please enter your Directv's port number.", required:true, defaultValue:"8080")
        }
    }

    page(name:"channelPage", title:"Install Virtual Devices", nextPage: "hubPage" ){
        section("Channel Name"){
            label(name:"label", title: "Name This channel", required: true, multiple: false, defaultValue:"NBC")
            input("channel", "text", title: "Channel Number", description: "Please enter channel number to tune to.", required:true, defaultValue:"12")
        }
    }

    page(name:"hubPage", title:"Install Virtual Devices", install: true, uninstall: true){

        section("on this hub...") {
            input "theHub", "hub", multiple: false, required: true
        }
    }

}

def installed() {
	log.debug "Installed with settings: ${settings}"
	state.dtvip = recip
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {

    def DNI = "DTV${convertToHex(channel)}"

    //try{

        //Get a current list of cameras
        def channelList = getChildDevices()

        if (channelList) {
            removeChildDevices(channelList)
        }

        def childDevice = addChildDevice("macmedia", "Directv IP Tuner", DNI, theHub.id, [name: "TV "+ app.label, label: "TV "+app.label, devicechannel:channel, completedSetup: true])

    //} catch (e) {
    //    log.error("Child -> Error triggered in init: $e")
    //}
}



private String convertIPtoHex(ipAddress) {
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex.toUpperCase()
}

private String convertToHex(port) {
    String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport.toUpperCase()
}

private removeChildDevices(delete) {
	log.info("Child -> removeChildDevices")
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}
