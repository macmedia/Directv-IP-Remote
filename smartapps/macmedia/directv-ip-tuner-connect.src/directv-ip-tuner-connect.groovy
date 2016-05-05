/**
 *  Directv IP Tuner (Connect)
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
    name: "Directv IP Tuner (Connect)",
    namespace: "macmedia",
    author: "Mike Elser",
    description: "Using the Directv IP Tuner Smart App will create a new virtual switch that can be used\r\nby Amazon Echo to say voice commands to turn to the select channel.\r\n\r\n(Sample: Alexa, turn on TV NBC)\r\nChannel Name: NBC  // Is used as phrase for Alexa\r\nChannel Number: 12  // Is used as channel to tune to",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/macmedia/Directv-IP-Tuner/master/Icons/DIRECTV.png",
    iconX2Url: "https://raw.githubusercontent.com/macmedia/Directv-IP-Tuner/master/Icons/DIRECTV@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/macmedia/Directv-IP-Tuner/master/Icons/DIRECTV@2x.png")


preferences {
    page(name: "mainPage", title: "Existing Channels", install: true, uninstall: true) {
        if(state?.installed) {
            section("Add a New Channel") {
                app "Directv IP Tuner Child", "macmedia", "Directv IP Tuner Child", title: "New Channel", page: "mainPage", multiple: true, install: true
            }
        } else {
            section("Initial Install") {
                paragraph "This smartapp installs virtual devices that can be controlled by the Amazon Alexa voice commands. Example: Alexa, turn on TV NBC."
            }
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
	state.installed = true


}

def uninstall(){
	log.debug "Parent Uninstall"
}
