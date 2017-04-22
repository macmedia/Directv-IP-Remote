/**
 *  Directv IP Remote Child
 *  Version 1.0.1 - 04/22/2017
 *
 *  Copyright 2017 Mike Elser
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
    name: "Directv IP Remote Child",
    namespace: "macmedia",
    author: "Mike Elser",
    description: "Direct IP Remote child app. This child app is called from Direct IP Remote (Connect) to add new channel virtual devices.",
    category: "Convenience",
    parent: "macmedia:Directv IP Remote (Connect)",
    iconUrl: "https://raw.githubusercontent.com/macmedia/Directv-IP-Remote/master/Icons/DIRECTV.png",
    iconX2Url: "https://raw.githubusercontent.com/macmedia/Directv-IP-Remote/master/Icons/DIRECTV@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/macmedia/Directv-IP-Remote/master/Icons/DIRECTV@2x.png")

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
    //Set a unique device network id
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
    def childDevice = addChildDevice("macmedia", "Directv IP Remote Device", DNI, hub, [name: "Channel "+ app.label, label: "Channel "+app.label, devicechannel:channel, completedSetup: true])

}

private String getSelectedBoxIP(){
	return parent.getSelectedBoxIP()
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
def uninstalled() {
    removeChildDevices(getChildDevices())
}
