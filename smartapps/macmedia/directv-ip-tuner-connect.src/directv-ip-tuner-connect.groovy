/**
*  Directv IP Remote
*  Version 1.0.0 - 05/05/2016
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
    name: "Directv IP Remote",
    namespace: "macmedia",
    author: "Mike Elser",
    description: "Creates virtual divice to connect to Alexa. After discovery you cal say 'Alexa, turn on TV NBC' and it will change the station.",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/macmedia/Directv-IP-Tuner/master/Icons/DIRECTV.png",
    iconX2Url: "https://raw.githubusercontent.com/macmedia/Directv-IP-Tuner/master/Icons/DIRECTV@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/macmedia/Directv-IP-Tuner/master/Icons/DIRECTV@2x.png")


preferences {
    page(name:"mainPage", title:"Directv Box Setup", content:"mainPage", refreshTimeout:5)
    page(name:"discoveryBoxes", title:"Directv Discovery", content:"discoveryBoxes", refreshTimeout:5)
    page(name:"channelSetupPage")

}

def mainPage(){
    log.debug "mainPage"
    def dtvBoxes = boxesDiscovered()

    discoveryBoxes()
}

def channelSetupPage(){
    dynamicPage(name: "channelSetupPage", title: "Existing Channels", install: false, uninstall: false) {
        if(state?.installed) {
            section("Add a New Channel") {
                app "Directv IP Tuner Child", "macmedia", "Directv IP Tuner Child", title: "New Channel", page: "channelPage", multiple: true, install: true
            }
        } else {
            section("Initial Install") {
                paragraph "This smartapp installs virtual devices that can be controlled by the Amazon Alexa voice commands. Example: Alexa, turn on TV NBC."
            }
        }
    }
}

def discoveryBoxes(params=[:]) {

    def dtvBoxes = boxesDiscovered()
    int bridgeRefreshCount = !state.bridgeRefreshCount ? 0 : state.bridgeRefreshCount as int
        state.bridgeRefreshCount = bridgeRefreshCount + 1
    def options = dtvBoxes ?: []
    def numFound = options.size() ?: 0
    def refreshInterval = 3

    ssdpSubscribe()

    //discovery request every 15 //25 seconds
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
        section("Channels"){
            href(page: "channelSetupPage", title: "Element: 'app'")
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    state.installed = true
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
    def dtvBoxes = getDirectvBoxes().findAll { it?.value?.verified == true }
    def map = [:]

    dtvBoxes.each {
        def value = it.value.name ?: "Directv ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
        def key = it.value.mac
        map["${key}"] = value
    }
    map
}

def getDirectvBoxes(){
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
        directvDevices = getDirectvBoxes()


        def getDirectvBox = directvDevices.find {it?.key?.contains(body?.device?.UDN?.text())}
        if (getDirectvBox) {
            getDirectvBox.value << [name:"${deviceType}-${deviceName}", verified: true]
        } else {
            log.error "XML returned a device that didn't exist"
        }
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
