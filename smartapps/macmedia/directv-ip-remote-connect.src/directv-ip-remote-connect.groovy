/**
*  Directv IP Remote (Connect)
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
    name: "Directv IP Remote (Connect)",
    namespace: "macmedia",
    author: "Mike Elser",
    description: "Directv Connect app to create virtual devices that can be contolled by Alexa.",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/macmedia/Directv-IP-Remote/master/Icons/DIRECTV.png",
    iconX2Url: "https://raw.githubusercontent.com/macmedia/Directv-IP-Remote/master/Icons/DIRECTV%402x.png",
    iconX3Url: "https://raw.githubusercontent.com/macmedia/Directv-IP-Remote/master/Icons/DIRECTV%402x.png",
    singleInstance: true)

def appVersion() { "1.0.0" }
def appVerDate() { "5-6-2016" }

preferences {
    page(name:"mainPage", title:"Directv Box Setup", content:"mainPage", refreshTimeout:5)
    page(name:"discoveryBoxes", title:"Directv Discovery", content:"discoveryBoxes", refreshTimeout:5)
    page(name:"channelSetupPage")
}

def mainPage(){
    def dtvBoxes = boxesDiscovered()
    discoveryBoxes()
}

def channelSetupPage(){
    dynamicPage(name: "channelSetupPage", title: "Existing Channels") {
        section("Add a New Channel") {
            app(name:"Directv IP Remote", appName:"Directv IP Remote Child", namespace:"macmedia", title: "New Channel", page: "channelPage", multiple: true)
        }
    }
}

def discoveryBoxes(params=[:]) {
    def dtvBoxes = boxesDiscovered()
    int dtvBoxesRefreshCount = !state.dtvBoxesRefreshCount ? 0 : state.dtvBoxesRefreshCount as int
    def options = dtvBoxes ?: []
    def numFound = options.size() ?: 0
    def refreshInterval = 3

    state.dtvBoxesRefreshCount = dtvBoxesRefreshCount + 1

    //Clean up
    if (numFound == 0 && state.dtvBoxesRefreshCount > 25) {
        log.trace "Cleaning old memory"
        state.dtvBoxes = [:]
        state.dtvBoxesRefreshCount = 0
        app.updateSetting("selectedBox", "")
    }

    //Subscribe to ssdp request
    ssdpSubscribe()

    //Discovery request every 15 //25 seconds
    if((dtvBoxesRefreshCount % 5) == 0) {
        log.info("discoverBoxes")
        discoverBoxes()
    }else{
        log.info("skipping discoverBoxes")
    }

    //Request every 5 seconds except on discoveries
    if(((dtvBoxesRefreshCount % 1) == 0) && ((dtvBoxesRefreshCount % 5) != 0)) {
        verifyDevices()
    }

    def boxesDiscovered = boxesDiscovered()

    return dynamicPage(name: "discoveryBoxes", title:"Discovery Started!", refreshInterval:refreshInterval, install:true, uninstall: true) {
        section("") {
            paragraph "Version: ${appVersion()}\nDate: ${appVerDate()}", image: "https://raw.githubusercontent.com/macmedia/Directv-IP-Remote/master/Icons/DIRECTV.png"
        }
        section("Please wait while we discover your Directv Boxes.") {
            input "selectedBox",
                "enum",
                required:false,
                title:"Select Directv Box (${boxesDiscovered.size()} found)",
                multiple:false,
                options:options
        }
        section("Add Channels"){
            href(page: "channelSetupPage", title: "Channel Setup")
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
    //def vmotions = switches.findAll { it?.verified == true }
    //log.trace "MOTIONS HERE: ${vmotions}"
    def dtvBoxes = getDirectvBoxes().findAll { it?.value?.verified == true }
    //log.debug("Boxes: $dtvBoxes")
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
    if(URI == "/0/description.xml"){
        sendHubCommand(new physicalgraph.device.HubAction("""GET $URI HTTP/1.1\r\nHOST: $deviceNetworkId\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}", [callback: "setupHandler"]))
    }
}

void setupHandler(hubResponse) {
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

def uninstalled() {
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}
