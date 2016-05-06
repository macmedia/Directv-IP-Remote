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
    //parent: "macmedia:Directv IP Remote",
    iconUrl: "https://raw.githubusercontent.com/macmedia/Directv-IP-Tuner/master/Icons/DIRECTV.png",
    iconX2Url: "https://raw.githubusercontent.com/macmedia/Directv-IP-Tuner/master/Icons/DIRECTV@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/macmedia/Directv-IP-Tuner/master/Icons/DIRECTV@2x.png")


preferences {

    page(name:"channelPage", title:"Install Virtual Devices", install: true, uninstall: true){
        section("Channel Name"){
            label(name:"label", title: "Name this channel", required: true, multiple: false, defaultValue:"NBC")
            input("channel", "text", title: "Channel Number", description: "Please enter channel number to tune to.", required:true, defaultValue:"12")
        }
    }
    
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {

    def DNI = "DTV${convertToHex(channel)}"

    //Get a current list of channels
    def channelList = getChildDevices()

    //Remove channel is we alread have it
    if (channelList) {
        removeChildDevices(channelList)
    }

    def macNumber = parent.settings.selectedBox
    def directvDevices = parent.getDirectvBoxes()
    def boxes = directvDevices.findAll { it?.value?.mac == macNumber }

    def ip = boxes.collect{it?.value?.ip}[0]
    def hub = boxes.collect{it?.value?.hub}[0]

	//Save state variables
	state.dtvip  = ip
    state.dtmac  = macNumber
    state.dtvhub = hub

	//Add virtual switch to things
    def childDevice = addChildDevice("macmedia", "Directv IP Tuner", DNI, hub, [name: "TV "+ app.label, label: "TV "+app.label, devicechannel:channel, completedSetup: true])

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
