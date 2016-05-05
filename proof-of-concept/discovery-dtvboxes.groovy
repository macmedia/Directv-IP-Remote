/**
 *  discover directv
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
    name: "discover directv",
    namespace: "macmedia",
    author: "Mike Elser",
    description: "look for boxes - test",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name:"mainPage", title:"Hue Device Setup", content:"mainPage", refreshTimeout:5)
    page(name:"discoveryBoxes", title:"Directv Discovery", content:"discoveryBoxes", refreshTimeout:5)
}

def mainPage(){
	log.debug "mainPage"
    def dtvBoxes = boxesDiscovered()
    
	discoveryBoxes()
}




def discoveryBoxes(params=[:]) {

	def bridges = boxesDiscovered()
    int bridgeRefreshCount = !state.bridgeRefreshCount ? 0 : state.bridgeRefreshCount as int
	state.bridgeRefreshCount = bridgeRefreshCount + 1
	def options = bridges ?: []
	def numFound = options.size() ?: 0
    def refreshInterval = 3

    
    ssdpSubscribe()
    
    //bridge discovery request every 15 //25 seconds
    if((bridgeRefreshCount % 5) == 0) {
    	log.info("discoverBoxes")
        discoverBoxes()
    }else{
    	log.info("skipping discoverBoxes")
    }
    
    
    //setup.xml request every 5 seconds except on discoveries
    if(((bridgeRefreshCount % 1) == 0) && ((bridgeRefreshCount % 5) != 0)) {
        verifyDevices()
    }
    
    def boxesDiscovered = boxesDiscovered()
    
	return dynamicPage(name: "discoveryBoxes", title:"Discovery Started!", refreshInterval:refreshInterval, install:true, uninstall: true) {
		section("Please wait while we discover your Directv Boxes.") {
			input "selectedBox", 
			"enum", 
			required:false, 
			title:"Select Directv Box (${boxesDiscovered.size()} found)", 
			multiple:false, 
			options:options
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
	// TODO: subscribe to attributes, devices, locations, etc.
}

private discoverBoxes() {
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:MediaServer:1", physicalgraph.device.Protocol.LAN))
}

void ssdpSubscribe() {
    subscribe(location, "ssdpTerm.urn:schemas-upnp-org:device:MediaServer:1", ssdpBoxHandler)
}

def ssdpBoxHandler(evt) {
	log.info("ssdp Box Handler fired")
    def description = evt.description
    def hub = evt?.hubId
    def parsedEvent = parseDiscoveryMessage(description)
    parsedEvent << ["hub":hub]
	log.debug parsedEvent
    
    def switches = getDirectvBoxes()
    if (!(switches."${parsedEvent.ssdpUSN.toString()}")) {
        //if it doesn't already exist
        switches << ["${parsedEvent.ssdpUSN.toString()}":parsedEvent]
    } else {
        log.debug "Device was already found in state..."
        def d = switches."${parsedEvent.ssdpUSN.toString()}"
        boolean deviceChangedValues = false
        log.debug "$d.ip <==> $parsedEvent.ip"
        if(d.ip != parsedEvent.ip || d.port != parsedEvent.port) {
            d.ip = parsedEvent.ip
            d.port = parsedEvent.port
            deviceChangedValues = true
            log.debug "Device's port or ip changed..."
            def child = getChildDevice(parsedEvent.mac)
            if (child) {
                child.subscribe(parsedEvent.ip, parsedEvent.port)
                child.poll()
            } else {
                log.debug "Device with mac $parsedEvent.mac not found"
            }
        }
    }
}

def boxesDiscovered() {
	//def vmotions = switches.findAll { it?.verified == true }
	//log.trace "MOTIONS HERE: ${vmotions}"
	def dtvBoxes = getDirectvBoxes().findAll { it?.value?.ssdpPath == "/0/description.xml" }
    //log.debug("Boxes: $dtvBoxes")
	def map = [:]
 
	dtvBoxes.each {
		def value = it.value.name ?: "WeMo Light Switch ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
		def key = it.value.mac
		map["${key}"] = value
	}
	map
}

def getDirectvBoxes()
{
	if (!state.dtvBoxes) { state.dtvBoxes = [:] }
	state.dtvBoxes
}

private verifyDevices() {
	def switches = getDirectvBoxes().findAll { it?.value?.verified != true }
	switches.each {
		getFriendlyName((it.value.ip + ":" + it.value.port), it.value.ssdpPath)
	}
}

private getFriendlyName(String deviceNetworkId, String URI) {
    log.debug("IP: $deviceNetworkId -- URI: $URI")
    log.debug("URI: $URI")
    if(URI == "/0/description.xml"){
        sendHubCommand(new physicalgraph.device.HubAction("""GET $URI HTTP/1.1\r\nHOST: $deviceNetworkId\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}", [callback: "setupHandler"]))
    }
}

void setupHandler(hubResponse) {
	//log.debug("Fire returned: ${hubResponse.headers['Content-Type']}")
	String contentType = hubResponse?.headers['Content-Type']
	if (contentType != null && contentType == 'text/xml;charset="utf-8"') {
		def body = hubResponse.xml
		def directvDevices = []
        String deviceType = body?.device?.manufacturer?.text() ?: ""
        String deviceName = body?.device['directv-hmc']?.text() ?: ""
        log.info("DeviceName: ${deviceType}-${deviceName}")
        directvDevices = getDirectvBoxes()

        
        def getDirectvBox = directvDevices.find {it?.key?.contains(body?.device?.UDN?.text())}
        if (getDirectvBox) {
			getDirectvBox.value << [name:"${deviceType}-${deviceName}", verified: true]
		} else {
			log.error "XML returned a device that didn't exist"
		}
        /*
		String deviceType = body?.device?.deviceType?.text() ?: ""
		if (deviceType.startsWith("urn:Belkin:device:controllee:1") || deviceType.startsWith("urn:Belkin:device:insight:1")) {
			wemoDevices = getWemoSwitches()
		} else if (deviceType.startsWith("urn:Belkin:device:sensor")) {
			wemoDevices = getWemoMotions()
		} else if (deviceType.startsWith("urn:Belkin:device:lightswitch")) {
			wemoDevices = getWemoLightSwitches()
		}

		def wemoDevice = wemoDevices.find {it?.key?.contains(body?.device?.UDN?.text())}
		if (wemoDevice) {
			wemoDevice.value << [name:body?.device?.friendlyName?.text(), verified: true]
		} else {
			log.error "/setup.xml returned a wemo device that didn't exist"
		}
        */
	}
}

private def parseDiscoveryMessage(String description) {
	def event = [:]
	def parts = description.split(',')
    log.debug("DESC: $description")
	parts.each { part ->
		part = part.trim()
		if (part.startsWith('devicetype:')) {
			part -= "devicetype:"
			event.devicetype = part.trim()
		}
		else if (part.startsWith('mac:')) {
			part -= "mac:"
			event.mac = part.trim()
		}
		else if (part.startsWith('networkAddress:')) {
			part -= "networkAddress:"
			event.ip = part.trim()
		}
		else if (part.startsWith('deviceAddress:')) {
			part -= "deviceAddress:"
			event.port = part.trim()
		}
		else if (part.startsWith('ssdpPath:')) {
			part -= "ssdpPath:"
			event.ssdpPath = part.trim()
		}
		else if (part.startsWith('ssdpUSN:')) {
			part -= "ssdpUSN:"
			event.ssdpUSN = part.trim()
		}
		else if (part.startsWith('ssdpTerm:')) {
			part -= "ssdpTerm:"
			event.ssdpTerm = part.trim()
		}
		else if (part.startsWith('headers')) {
			part -= "headers:"
			event.headers = part.trim()
		}
		else if (part.startsWith('body')) {
			part -= "body:"
			event.body = part.trim()
		}
	}
	event
}