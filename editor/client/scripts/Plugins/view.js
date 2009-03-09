/** * Copyright (c) 2006 * Martin Czuchra, Nicolas Peters, Daniel Polak, Willi Tscheschner * * Permission is hereby granted, free of charge, to any person obtaining a * copy of this software and associated documentation files (the "Software"), * to deal in the Software without restriction, including without limitation * the rights to use, copy, modify, merge, publish, distribute, sublicense, * and/or sell copies of the Software, and to permit persons to whom the * Software is furnished to do so, subject to the following conditions: * * The above copyright notice and this permission notice shall be included in * all copies or substantial portions of the Software. * * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER * DEALINGS IN THE SOFTWARE. **/if(!ORYX.Plugins)	ORYX.Plugins = new Object();ORYX.Plugins.View = Clazz.extend({	offlineActive: true,	facade: undefined,	construct: function(facade) {		this.facade = facade;		this.zoomLevel = 1.0;		this.minZoomLevel = 0.21;		this.maxZoomLevel = 2.50;		this.facade.offer({			'name':ORYX.I18N.View.zoomIn,			'functionality': this.zoom.bind(this, [1.0 + ORYX.CONFIG.ZOOM_OFFSET]),			'group': ORYX.I18N.View.group,			'icon': ORYX.PATH + "images/magnifier_zoom_in.png",			'description': ORYX.I18N.View.zoomInDesc,			'index': 1,			'minShape': 0,			'maxShape': 0,			'isEnabled': function(){return this.zoomLevel < this.maxZoomLevel }.bind(this)});		this.facade.offer({			'name':ORYX.I18N.View.zoomOut,			'functionality': this.zoom.bind(this, [1.0 - ORYX.CONFIG.ZOOM_OFFSET]),			'group': ORYX.I18N.View.group,			'icon': ORYX.PATH + "images/magnifier_zoom_out.png",			'description': ORYX.I18N.View.zoomOutDesc,			'index': 2,			'minShape': 0,			'maxShape': 0,			'isEnabled': function(){ return this.zoomLevel > this.minZoomLevel }.bind(this)});	},	zoom: function(faktor) {		// TODO: Zoomen auf allen Objekten im SVG-DOM		this.zoomLevel *= faktor;				var canvas = this.facade.getCanvas();					var newWidth 	= canvas.bounds.width()  * this.zoomLevel;		var newHeight 	= canvas.bounds.height() * this.zoomLevel;		// Set new top offset		var offsetTop = (canvas.node.parentNode.parentNode.parentNode.offsetHeight - newHeight) / 2.0;				offsetTop = offsetTop > 20 ? offsetTop - 20 : 0;		canvas.node.parentNode.parentNode.style.marginTop = offsetTop + "px";		offsetTop += 5;		canvas.getHTMLContainer().style.top = offsetTop + "px";				// Set new Zoom-Level		canvas.setSize({width: newWidth, height: newHeight}, true);		// Set Scale-Factor		canvas.node.setAttributeNS(null, "transform", "scale(" +this.zoomLevel+ ")");				// Refresh the Selection		this.facade.updateSelection();	}});