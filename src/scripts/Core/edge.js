/** * Copyright (c) 2006 * Martin Czuchra, Nicolas Peters, Daniel Polak, Willi Tscheschner * * Permission is hereby granted, free of charge, to any person obtaining a * copy of this software and associated documentation files (the "Software"), * to deal in the Software without restriction, including without limitation * the rights to use, copy, modify, merge, publish, distribute, sublicense, * and/or sell copies of the Software, and to permit persons to whom the * Software is furnished to do so, subject to the following conditions: * * The above copyright notice and this permission notice shall be included in * all copies or substantial portions of the Software. * * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER * DEALINGS IN THE SOFTWARE. **/NAMESPACE_SVG = "http://www.w3.org/2000/svg";NAMESPACE_ORYX = "http://www.b3mn.org/oryx";/** * Init namespaces */if (!ORYX) {    var ORYX = {};}if (!ORYX.Core) {    ORYX.Core = {};}/** * @classDescription Abstract base class for all connections. * @extends {ORYX.Core.Shape} * @param options {Object} * * TODO da die verschiebung der Edge nicht �ber eine *  translation gemacht wird, die sich auch auf alle kind UIObjects auswirkt, *  m�ssen die kinder hier beim verschieben speziell betrachtet werden. *  Das sollte �berarbeitet werden. * */ORYX.Core.Edge = {    /**     * Constructor     * @param {Object} options     * @param {Stencil} stencil     */    construct: function(options, stencil){        arguments.callee.$.construct.apply(this, arguments);                this.isMovable = true;        this.isSelectable = true;                this._markers = $H(); //a hash map of SVGMarker objects where keys are the marker ids        this._paths = [];        this._interactionPaths = [];        this._dockersByPath = $H();        this._markersByPath = $H();                //TODO was muss hier initial erzeugt werden?        var stencilNode = this.node.childNodes[0].childNodes[0];        stencilNode = ORYX.Editor.graft("http://www.w3.org/2000/svg", stencilNode, ['g', {            "pointer-events": "painted"        }]);                //Add to the EventHandler        this.addEventHandlers(stencilNode);                        this._oldBounds = this.bounds.clone();                //load stencil        this._init(this._stencil.view());                if (stencil instanceof Array) {            this.deserialize(stencil);        }            },        _update: function(dockerUpdated){				if(dockerUpdated || this.isChanged) {	        	        if (this.bounds.width() === 0 || this.bounds.height() === 0) {	        	            this.bounds.moveBy({	                x: this.bounds.width() === 0 ? -1 : 0,	                y: this.bounds.height() === 0 ? -1 : 0	            });	            	            this.bounds.extend({	                x: this.bounds.width() === 0 ? 2 : 0,	                y: this.bounds.height() === 0 ? 2 : 0	            });	            	        }	        	        // TODO: Bounds muss abhaengig des Eltern-Shapes gesetzt werden	        var upL = this.bounds.upperLeft();	        var oldUpL = this._oldBounds.upperLeft();	        var oldWidth = this._oldBounds.width() === 0 ? this.bounds.width() : this._oldBounds.width();	        var oldHeight = this._oldBounds.height() === 0 ? this.bounds.height() : this._oldBounds.height();	        var diffX = upL.x - oldUpL.x;	        var diffY = upL.y - oldUpL.y;	        var diffWidth = this.bounds.width() / oldWidth;	        var diffHeight = this.bounds.height() / oldHeight;	        	        this.dockers.each((function(docker){	            // Unregister on BoundsChangedCallback	            docker.bounds.unregisterCallback(this._dockerChangedCallback);	            	            // If there is any changes at the edge and is there is not an DockersUpdate	            // set the new bounds to the docker	            if (!dockerUpdated) {	                docker.bounds.moveBy(diffX, diffY);	                	                if (diffWidth !== 1 || diffHeight !== 1) {	                    var relX = docker.bounds.upperLeft().x - upL.x;	                    var relY = docker.bounds.upperLeft().y - upL.y;	                    	                    docker.bounds.moveTo(upL.x + relX * diffWidth, upL.y + relY * diffHeight);	                }	            }	            // Do Docker update and register on DockersBoundChange	            docker.update();	            docker.bounds.registerCallback(this._dockerChangedCallback);	            	        }).bind(this));	        	        if (dockerUpdated) {	            var a = this.dockers.first().bounds.center();	            var b = this.dockers.first().bounds.center();	            	            this.dockers.each((function(docker){	                var center = docker.bounds.center();	                a.x = Math.min(a.x, center.x);	                a.y = Math.min(a.y, center.y);	                b.x = Math.max(b.x, center.x);	                b.y = Math.max(b.y, center.y);	            }).bind(this));	            	            //set the bounds of the the association	            this.bounds.set(Object.clone(a), Object.clone(b));	        }						//reposition labels			this.getLabels().each(function(label) {				switch (label.edgePosition) {					case "starttop":						var angle = this._getAngle(this.dockers[0], this.dockers[1]);						var pos = this.dockers.first().bounds.center();												if (angle <= 90 || angle > 270) {							label.horizontalAlign("left");							label.verticalAlign("bottom");							label.x = pos.x + ORYX.CONFIG.OFFSET_EDGE_LABEL_TOP;							label.y = pos.y - ORYX.CONFIG.OFFSET_EDGE_LABEL_TOP;							label.rotate(360 - angle, pos);						} else {							label.horizontalAlign("right");							label.verticalAlign("bottom");							label.x = pos.x - ORYX.CONFIG.OFFSET_EDGE_LABEL_TOP;							label.y = pos.y - ORYX.CONFIG.OFFSET_EDGE_LABEL_TOP;							label.rotate(180 - angle, pos);						}												break;					case "startbottom":						var angle = this._getAngle(this.dockers[0], this.dockers[1]);						var pos = this.dockers.first().bounds.center();												if (angle <= 90 || angle > 270) {							label.horizontalAlign("left");							label.verticalAlign("top");							label.x = pos.x + ORYX.CONFIG.OFFSET_EDGE_LABEL_BOTTOM;							label.y = pos.y + ORYX.CONFIG.OFFSET_EDGE_LABEL_BOTTOM;							label.rotate(360 - angle, pos);						} else {							label.horizontalAlign("right");							label.verticalAlign("top");							label.x = pos.x - ORYX.CONFIG.OFFSET_EDGE_LABEL_BOTTOM;							label.y = pos.y + ORYX.CONFIG.OFFSET_EDGE_LABEL_BOTTOM;							label.rotate(180 - angle, pos);						}												break;					case "midtop":						var numOfDockers = this.dockers.length;						if(numOfDockers%2 == 0) {							var angle = this._getAngle(this.dockers[numOfDockers/2-1], this.dockers[numOfDockers/2])							var pos1 = this.dockers[numOfDockers/2-1].bounds.center();							var pos2 = this.dockers[numOfDockers/2].bounds.center();							var pos = {x:(pos1.x + pos2.x)/2.0, y:(pos1.y+pos2.y)/2.0};														label.horizontalAlign("center");							label.verticalAlign("bottom");							label.x = pos.x;							label.y = pos.y - ORYX.CONFIG.OFFSET_EDGE_LABEL_TOP;															if (angle <= 90 || angle > 270) {								label.rotate(360 - angle, pos);							} else {								label.rotate(180 - angle, pos);							}						} else {							var index = parseInt(numOfDockers/2);							var angle = this._getAngle(this.dockers[index], this.dockers[index+1])							var pos = this.dockers[index].bounds.center();														if (angle <= 90 || angle > 270) {								label.horizontalAlign("left");								label.verticalAlign("bottom");								label.x = pos.x + ORYX.CONFIG.OFFSET_EDGE_LABEL_TOP;								label.y = pos.y - ORYX.CONFIG.OFFSET_EDGE_LABEL_TOP;								label.rotate(360 - angle, pos);							} else {								label.horizontalAlign("right");								label.verticalAlign("bottom");								label.x = pos.x - ORYX.CONFIG.OFFSET_EDGE_LABEL_TOP;								label.y = pos.y - ORYX.CONFIG.OFFSET_EDGE_LABEL_TOP;								label.rotate(180 - angle, pos);							}						}												break;					case "midbottom":						var numOfDockers = this.dockers.length;						if(numOfDockers%2 == 0) {							var angle = this._getAngle(this.dockers[numOfDockers/2-1], this.dockers[numOfDockers/2])							var pos1 = this.dockers[numOfDockers/2-1].bounds.center();							var pos2 = this.dockers[numOfDockers/2].bounds.center();							var pos = {x:(pos1.x + pos2.x)/2.0, y:(pos1.y+pos2.y)/2.0};														label.horizontalAlign("center");							label.verticalAlign("top");							label.x = pos.x;							label.y = pos.y + ORYX.CONFIG.OFFSET_EDGE_LABEL_TOP;														if (angle <= 90 || angle > 270) {								label.rotate(360 - angle, pos);							} else {								label.rotate(180 - angle, pos);							}						} else {							var index = parseInt(numOfDockers/2);							var angle = this._getAngle(this.dockers[index], this.dockers[index+1])							var pos = this.dockers[index].bounds.center();														if (angle <= 90 || angle > 270) {								label.horizontalAlign("left");								label.verticalAlign("top");								label.x = pos.x + ORYX.CONFIG.OFFSET_EDGE_LABEL_BOTTOM;								label.y = pos.y + ORYX.CONFIG.OFFSET_EDGE_LABEL_BOTTOM;								label.rotate(360 - angle, pos);							} else {								label.horizontalAlign("right");								label.verticalAlign("top");								label.x = pos.x - ORYX.CONFIG.OFFSET_EDGE_LABEL_BOTTOM;								label.y = pos.y + ORYX.CONFIG.OFFSET_EDGE_LABEL_BOTTOM;								label.rotate(180 - angle, pos);							}						}												break;					case "endtop":						var length = this.dockers.length;						var angle = this._getAngle(this.dockers[length-2], this.dockers[length-1]);						var pos = this.dockers.last().bounds.center();												if (angle <= 90 || angle > 270) {							label.horizontalAlign("right");							label.verticalAlign("bottom");							label.x = pos.x - ORYX.CONFIG.OFFSET_EDGE_LABEL_TOP;							label.y = pos.y - ORYX.CONFIG.OFFSET_EDGE_LABEL_TOP;							label.rotate(360 - angle, pos);						} else {							label.horizontalAlign("left");							label.verticalAlign("bottom");							label.x = pos.x + ORYX.CONFIG.OFFSET_EDGE_LABEL_TOP;							label.y = pos.y - ORYX.CONFIG.OFFSET_EDGE_LABEL_TOP;							label.rotate(180 - angle, pos);						}												break;					case "endbottom":						var length = this.dockers.length;						var angle = this._getAngle(this.dockers[length-2], this.dockers[length-1]);						var pos = this.dockers.last().bounds.center();												if (angle <= 90 || angle > 270) {							label.horizontalAlign("right");							label.verticalAlign("top");							label.x = pos.x - ORYX.CONFIG.OFFSET_EDGE_LABEL_BOTTOM;							label.y = pos.y + ORYX.CONFIG.OFFSET_EDGE_LABEL_BOTTOM;							label.rotate(360 - angle, pos);						} else {							label.horizontalAlign("left");							label.verticalAlign("top");							label.x = pos.x + ORYX.CONFIG.OFFSET_EDGE_LABEL_BOTTOM;							label.y = pos.y + ORYX.CONFIG.OFFSET_EDGE_LABEL_BOTTOM;							label.rotate(180 - angle, pos);						}												break;				}			}.bind(this));						this.refresh();						this.isChanged = false;						this._oldBounds = this.bounds.clone();        }    },        refresh: function(){        //call base class refresh method        arguments.callee.$.refresh.apply(this, arguments);                //TODO consider points for marker mids        var lastPoint;        this._paths.each((function(path, index){            var dockers = this._dockersByPath.get(path.id);                        if (lastPoint) {                var d = "M" + lastPoint.x + " " + lastPoint.y;            }            else {                var c = dockers[0].bounds.center();                lastPoint = c;                                var d = "M" + c.x + " " + c.y;            }                        for (var i = 1; i < dockers.length; i++) {                // for each docker, draw a line to the center                c = dockers[i].bounds.center();                d = d + "L" + c.x + " " + c.y + " ";                lastPoint = c;            }                        path.setAttributeNS(null, "d", d);            this._interactionPaths[index].setAttributeNS(null, "d", d);                    }).bind(this));    },        /**     * Calculate the Border Intersection Point between two points     * @param {PointA}     * @param {PointB}     */    getIntersectionPoint: function(){            var length = Math.floor(this.dockers.length / 2)                return ORYX.Core.Math.midPoint(this.dockers[length - 1].bounds.center(), this.dockers[length].bounds.center())    },            /**     * Calculate if the point is inside the Shape     * @param {PointX}     * @param {PointY}      */    isPointIncluded: function(pointX, pointY){            var isbetweenAB = this.absoluteBounds().isIncluded(pointX, pointY);                if (isbetweenAB && this.dockers.length > 0) {					var i = 0;			var point 	= {x: pointX, y: pointY};			var line 	= {point1: null,point2: null};			var isPointIncluded;						do {							line.point1 = this.dockers[i].bounds.center();				line.point2 = this.dockers[++i].bounds.center();								isPointIncluded = ORYX.Core.Math.isPointInLine(point, line, 5);							} while (!isPointIncluded && i < this.dockers.length - 1)					}				return isPointIncluded;    },          /**     * Calculate if the point is over an special offset area     * @param {Point}     */    isPointOverOffset: function(){		return  false	},		/**	* Returns the angle of the line between two dockers	* (0 - 359.99999999)	*/	_getAngle: function(docker1, docker2) {		var p1 = docker1.absoluteCenterXY();		var p2 = docker2.absoluteCenterXY();				if(p1.x == p2.x && p1.y == p2.y)			return 0;		var angle = Math.asin(Math.sqrt(Math.pow(p1.y-p2.y, 2))					/(Math.sqrt(Math.pow(p2.x-p1.x, 2)+Math.pow(p1.y-p2.y, 2))))					*180/Math.PI;				if(p2.x >= p1.x && p2.y <= p1.y)			return angle;		else if(p2.x < p1.x && p2.y <= p1.y)			return 180 - angle;		else if(p2.x < p1.x && p2.y > p1.y)			return 180 + angle;		else			return 360 - angle;	},	        alignDockers: function(){        this._update(true);                var firstPoint = this.dockers.first().bounds.center();        var lastPoint = this.dockers.last().bounds.center();                var deltaX = lastPoint.x - firstPoint.x;        var deltaY = lastPoint.y - firstPoint.y;                var numOfDockers = this.dockers.length - 1;                this.dockers.each((function(docker, index){            var part = index / numOfDockers;            docker.bounds.unregisterCallback(this._dockerChangedCallback);            docker.bounds.moveTo(firstPoint.x + part * deltaX, firstPoint.y + part * deltaY);            docker.bounds.registerCallback(this._dockerChangedCallback);        }).bind(this));                this._dockerChanged();    },        addDocker: function(position, exDocker){        var lastDocker;		var result;        this._dockersByPath.any((function(pair){            return pair.value.any((function(docker, index){                if (!lastDocker) {                    lastDocker = docker;                    return false;                }                else {                    var line = {                        point1: lastDocker.bounds.center(),                        point2: docker.bounds.center()                    };                    if (ORYX.Core.Math.isPointInLine(position, line, 10)) {                        var path = this._paths.find(function(path){                            return path.id === pair.key;                        });                        if (path) {                            var allowAttr = path.getAttributeNS(NAMESPACE_ORYX, 'allowDockers');                            if (allowAttr && allowAttr.toLowerCase() === "no") {                                return true;                            }                        }                        var newDocker = (exDocker) ? exDocker : this.createDocker();                        if(exDocker)							this.add(newDocker);						newDocker.bounds.centerMoveTo(position);                        pair.value.splice(index, 0, newDocker);                        this.dockers = this.dockers.without(newDocker);                        this.dockers.splice(this.dockers.indexOf(lastDocker) + 1, 0, newDocker);                        this._update(true);						result = newDocker;                        return true;                    }                    else {                        lastDocker = docker;                        return false;                    }                }            }).bind(this));        }).bind(this));		return result;    },        removeDocker: function(docker){        if (this.dockers.length > 2 && !(this.dockers.first() === docker)) {            this._dockersByPath.any((function(pair){                if (pair.value.member(docker)) {                    if (docker === pair.value.last()) {                        return true;                    }                    else {                        this.remove(docker);                        this._dockersByPath.set(pair.key, pair.value.without(docker));                        this.isChanged = true;                        this._dockerChanged();                        return true;                    }                }                return false;            }).bind(this));        }    },        /**     * Initializes the Edge after loading the SVG representation of the edge.     * @param {SVGDocument} svgDocument     */    _init: function(svgDocument){        arguments.callee.$._init.apply(this, arguments);                var minPoint, maxPoint;                //init markers        var defs = svgDocument.getElementsByTagNameNS(NAMESPACE_SVG, "defs");        if (defs.length > 0) {            defs = defs[0];            var markerElements = $A(defs.getElementsByTagNameNS(NAMESPACE_SVG, "marker"));            var marker;            var me = this;            markerElements.each(function(markerElement){                try {                    marker = new ORYX.Core.SVG.SVGMarker(markerElement.cloneNode(true));                    me._markers[marker.id] = marker;                    var textElements = $A(marker.element.getElementsByTagNameNS(NAMESPACE_SVG, "text"));                    var label;                    textElements.each(function(textElement){                        label = new ORYX.Core.SVG.Label({                            textElement: textElement,							shapeId: this.id                        });                        me._labels[label.id] = label;                    });                }                 catch (e) {                }            });        }                        var gs = svgDocument.getElementsByTagNameNS(NAMESPACE_SVG, "g");        if (gs.length <= 0) {            throw "Edge: No g element found.";        }        var g = gs[0];                        g.setAttributeNS(null, "id", null);                var isFirst = true;                $A(g.childNodes).each((function(path, index){            if (path instanceof SVGPathElement) {                path = path.cloneNode(false);                                var pathId = this.id + "_" + index;                path.setAttributeNS(null, "id", pathId);                this._paths.push(path);                                //check, if markers are set and update the id                var markersByThisPath = [];                var markerUrl = path.getAttributeNS(null, "marker-start");                                if (markerUrl !== "") {                    markerUrl = markerUrl.strip();                    markerUrl = markerUrl.replace(/^url\(#/, '');                    var markerStartId = this.id.concat(markerUrl.replace(/\)$/, ''));                    path.setAttributeNS(null, "marker-start", "url(#" + markerStartId + ")");                                        markersByThisPath.push(this._markers.get(markerStartId));                }                                markerUrl = path.getAttributeNS(null, "marker-mid");                                if (markerUrl !== "") {                    markerUrl = markerUrl.strip();                    markerUrl = markerUrl.replace(/^url\(#/, '');                    var markerMidId = this.id.concat(markerUrl.replace(/\)$/, ''));                    path.setAttributeNS(null, "marker-mid", "url(#" + markerMidId + ")");                                        markersByThisPath.push(this._markers.get(markerMidId));                }                                markerUrl = path.getAttributeNS(null, "marker-end");                                if (markerUrl !== "") {                    markerUrl = markerUrl.strip();                    markerUrl = markerUrl.replace(/^url\(#/, '');                    var markerEndId = this.id.concat(markerUrl.replace(/\)$/, ''));                    path.setAttributeNS(null, "marker-end", "url(#" + markerEndId + ")");                                        markersByThisPath.push(this._markers.get(markerEndId));                }                                this._markersByPath.set(pathId, markersByThisPath);                                //init dockers                var parser = new PathParser();                var handler = new ORYX.Core.SVG.PointsPathHandler();                parser.setHandler(handler);                parser.parsePath(path);                                if (handler.points.length < 2) {                    throw "Edge: Path has to have two or more points specified.";                }                                this._dockersByPath.set(pathId, []);                                handler.points.each((function(point, pIndex){                    if (isFirst || pIndex > 0) {                        var docker = new ORYX.Core.Controls.Docker({                            eventHandlerCallback: this.eventHandlerCallback                        });                        docker.bounds.centerMoveTo(point);						docker.bounds.registerCallback(this._dockerChangedCallback);                        this.add(docker);                                                this._dockersByPath.get(pathId).push(docker);                                                //calculate minPoint and maxPoint                        if (minPoint) {                            minPoint.x = Math.min(point.x, minPoint.x);                            minPoint.y = Math.min(point.y, minPoint.y);                        }                        else {                            minPoint = Object.clone(point);                        }                                                if (maxPoint) {                            maxPoint.x = Math.max(point.x, maxPoint.x);                            maxPoint.y = Math.max(point.y, maxPoint.y);                        }                        else {                            maxPoint = Object.clone(point);                        }                    }                }).bind(this));                                isFirst = false;            }        }).bind(this));                this.bounds.set(minPoint, maxPoint);                if (this.bounds.width() === 0 || this.bounds.height() === 0) {            this.bounds.extend({                x: this.bounds.width() === 0 ? 2 : 0,                y: this.bounds.height() === 0 ? 2 : 0            });                        this.bounds.moveBy({                x: this.bounds.width() === 0 ? -1 : 0,                y: this.bounds.height() === 0 ? -1 : 0            });                    }                this._oldBounds = this.bounds.clone();                //add paths to this.node        this._paths.reverse();        var paths = [];        this._paths.each((function(path){            paths.push(this.node.childNodes[0].childNodes[0].childNodes[0].appendChild(path));        }).bind(this));                this._paths = paths;                //init interaction path        this._paths.each((function(path){            var iPath = path.cloneNode(false);			iPath.setAttributeNS(null, "id", undefined);            iPath.setAttributeNS(null, "stroke-width", 10);            iPath.setAttributeNS(null, "visibility", "hidden");            iPath.setAttributeNS(null, "stroke-dasharray", null);            iPath.setAttributeNS(null, "stroke", "black");            iPath.setAttributeNS(null, "fill", "none");            this._interactionPaths.push(this.node.childNodes[0].childNodes[0].childNodes[0].appendChild(iPath));        }).bind(this));                this._paths.reverse();        this._interactionPaths.reverse();				/**initialize labels*/        var textElems = svgDocument.getElementsByTagNameNS(ORYX.CONFIG.NAMESPACE_SVG, 'text');        		$A(textElems).each((function(textElem){            var label = new ORYX.Core.SVG.Label({                textElement: textElem,				shapeId: this.id            });            this.node.childNodes[0].childNodes[0].appendChild(label.node);            this._labels.set(label.id, label);        }).bind(this)); 		        //set title        this.node.setAttributeNS(null, "title", this.getStencil().title());                this.propertiesChanged.each(function(pair){            pair.value = true;        });		        //this._update(true);    },        /**     * Adds all necessary markers of this Edge to the SVG document.     * Has to be called, while this.node is part of DOM.     */    addMarkers: function(defs){        this._markers.each(function(marker){            if (!defs.ownerDocument.getElementById(marker.value.id)) {                marker.value.element = defs.appendChild(marker.value.element);            }        });    },        /**     * Removes all necessary markers of this Edge from the SVG document.     * Has to be called, while this.node is part of DOM.     */    removeMarkers: function(){        var svgElement = this.node.ownerSVGElement;        if (svgElement) {            var defs = svgElement.getElementsByTagNameNS(NAMESPACE_SVG, "defs");            if (defs.length > 0) {                defs = defs[0];                this._markers.each(function(marker){                    var foundMarker = defs.ownerDocument.getElementById(marker.value.id);                    if (foundMarker) {                        marker.value.element = defs.removeChild(marker.value.element);                    }                });            }        }    },        /**     * Calls when a docker has changed     */    _dockerChanged: function(){            this._update(true);            },        serialize: function(){        var result = arguments.callee.$.serialize.apply(this);                //add dockers triple        var value = "";        this._dockersByPath.each((function(pair){            pair.value.each(function(docker){                var position = docker.getDockedShape() && docker.referencePoint ? docker.referencePoint : docker.bounds.center();                value = value.concat(position.x + " " + position.y + " ");            });                        value += " # ";        }).bind(this));        result.push({            name: 'dockers',            prefix: 'oryx',            value: value,            type: 'literal'        });                //add parent triple dependant on the dockedShapes        //TODO change this when canvas becomes a resource/*        var source = this.dockers.first().getDockedShape();        var target = this.dockers.last().getDockedShape();        var sharedParent;        if (source && target) {            //get shared parent            while (source.parent) {                source = source.parent;                if (source instanceof ORYX.Core.Canvas) {                    sharedParent = source;                    break;                }                else {                    var targetParent = target.parent;                    var found;                    while (targetParent) {                        if (source === targetParent) {                            sharedParent = source;                            found = true;                            break;                        }                        else {                            targetParent = targetParent.parent;                        }                    }                    if (found) {                        break;                    }                }            }        }        else             if (source) {                sharedParent = source.parent;            }            else                 if (target) {                    sharedParent = target.parent;                }*/                //if (sharedParent) {/*            result.push({                name: 'parent',                prefix: 'raziel',                //value: '#' + ERDF.__stripHashes(sharedParent.resourceId),                value: '#' + ERDF.__stripHashes(this.getCanvas().resourceId),                type: 'resource'            });*/        //}				//serialize target and source		var lastDocker = this.dockers.last();				var target = lastDocker.getDockedShape();				if(target) {			result.push({				name: 'target',				prefix: 'raziel',				value: '#' + ERDF.__stripHashes(target.resourceId),				type: 'resource'			});		}                try {            result = this.getStencil().serialize(this, result);        }         catch (e) {        }                return result;    },        deserialize: function(data){        try {            data = this.getStencil().deserialize(this, data);        }         catch (e) {        }        		// Set the outgoing shapes		var target = data.find(function(ser) {return (ser.prefix+"-"+ser.name) == 'raziel-target'});		var targetShape;		if(target) {			targetShape = this.getCanvas().getChildShapeByResourceId(target.value);		}				var outgoing = data.findAll(function(ser){ return (ser.prefix+"-"+ser.name) == 'raziel-outgoing'});		outgoing.each((function(obj){			// TODO: ID sind noch nicht verf�gbar, wenn neue Shapes hinzugef�gt worden sind.						// TODO: Look at Canvas			if(!this.parent) {return};											// Set outgoing Shape			var next = this.getCanvas().getChildShapeByResourceId(obj.value);																		if(next){				if(next == targetShape) {					// If this is an edge, set the last docker to the next shape					this.dockers.last().setDockedShape(next);					this.dockers.last().setReferencePoint({x: next.bounds.width() / 2.0, y: next.bounds.height() / 2.0});				} else if(next instanceof ORYX.Core.Edge) {					//Set the first docker of the next shape					next.dockers.first().setDockedShape(this);					//next.dockers.first().setReferencePoint({x: this.bounds.width() / 2.0, y: this.bounds.height() / 2.0});				} /*else if(next.dockers.length > 0) { //next is a node and next has a docker					next.dockers.first().setDockedShape(this);					next.dockers.first().setReferencePoint({x: this.bounds.width() / 2.0, y: this.bounds.height() / 2.0});				}*/			}						}).bind(this));		        arguments.callee.$.deserialize.apply(this, [data]);                var oryxDockers = data.find(function(obj){            return (obj.prefix === "oryx" &&            obj.name === "dockers");        });		        if (oryxDockers) {            var dataByPath = oryxDockers.value.split("#").without("").without(" ");                        dataByPath.each((function(data, index){                var values = data.replace(/,/g, " ").split(" ").without("");                                //for each docker two values must be defined                if (values.length % 2 === 0) {                    var path = this._paths[index];                                        if (path) {                        if (index === 0) {                            while (this._dockersByPath.get(path.id).length > 2) {                                this.removeDocker(this._dockersByPath[path.id][1]);                            }                        }                        else {                            while (this._dockersByPath.get(path.id).length > 1) {                                this.removeDocker(this._dockersByPath.get(path.id)[0]);                            }                        }                                                var dockersByPath = this._dockersByPath.get(path.id);                                                if (index === 0) {                            //set position of first docker                            var x = parseFloat(values.shift());                            var y = parseFloat(values.shift());                                                        if (dockersByPath.first().getDockedShape()) {                                dockersByPath.first().setReferencePoint({                                    x: x,                                    y: y                                });                            }                            else {                                dockersByPath.first().bounds.centerMoveTo(x, y);                            }                        }                                                //set position of last docker                        y = parseFloat(values.pop());                        x = parseFloat(values.pop());                                                if (dockersByPath.last().getDockedShape()) {                            dockersByPath.last().setReferencePoint({                                x: x,                                y: y                            });                        }                        else {                            dockersByPath.last().bounds.centerMoveTo(x, y);                        }                                                //add additional dockers                        for (var i = 0; i < values.length; i++) {                            x = parseFloat(values[i]);                            y = parseFloat(values[++i]);                                                        var newDocker = this.createDocker();                                                        newDocker.bounds.centerMoveTo(x, y);                                                        dockersByPath.splice(dockersByPath.length - 1, 0, newDocker);                            this.dockers = this.dockers.without(newDocker);                            this.dockers.splice(this.dockers.indexOf(dockersByPath.last()), 0, newDocker);                        }                    }                }            }).bind(this));        }        else {            this.alignDockers();        }		this._changed();    },        toString: function(){        return this.getStencil().title() + " " + this.id;    }};ORYX.Core.Edge = ORYX.Core.Shape.extend(ORYX.Core.Edge);