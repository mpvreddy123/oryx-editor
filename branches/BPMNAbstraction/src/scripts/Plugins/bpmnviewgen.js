/** * Copyright (c) 2006 * Martin Czuchra, Nicolas Peters, Daniel Polak, Willi Tscheschner * * Permission is hereby granted, free of charge, to any person obtaining a * copy of this software and associated documentation files (the "Software"), * to deal in the Software without restriction, including without limitation * the rights to use, copy, modify, merge, publish, distribute, sublicense, * and/or sell copies of the Software, and to permit persons to whom the * Software is furnished to do so, subject to the following conditions: * * The above copyright notice and this permission notice shall be included in * all copies or substantial portions of the Software. * * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER * DEALINGS IN THE SOFTWARE. **/if (!ORYX.Plugins)     ORYX.Plugins = new Object();ORYX.Plugins.BPMNViewGenerator = Clazz.extend({    facade: undefined,		processURI: undefined,	        construct: function(facade){        this.facade = facade;                        this.facade.offer({            'name': "Generate Abstract View",            'functionality': this.generateView.bind(this),            'group': "BPMN",            'icon': ORYX.PATH + "images/door.png",            'description': "Generate Abstract View",            'index': 0,            'minShape': 0,            'maxShape': 0        });    },          generateView: function(){            var resource = location.href;                // Get the serialized svg image source        var svgClone = this.facade.getCanvas().getRootNode().cloneNode(true);		        // Set the width and height        svgClone.setAttributeNS(null, 'width', 2800);        svgClone.setAttributeNS(null, 'height', 2200);                var svgDOM = DataManager.serialize(svgClone);                // Send the svg to the server.        //TODO make this better by using content negotiation instead of format parameter.        //TODO make this better by generating svg on the server, too.        new Ajax.Request(ORYX.CONFIG.PDF_EXPORT_URL, {            method: 'POST',            parameters: {                resource: resource,                data: svgDOM,                format: "pdf"            },            onSuccess: function(request){                            // Because the pdf may be opened in the same window as the                // process, yet the process may not have been saved, we're                // opening every other representation in a new window.                // location.href = request.responseText                window.open(request.responseText);            }        });    }});