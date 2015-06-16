"use strict";
(function (root, factory) {
    if (typeof define === "function" && define.amd) {
        define(["d3", "../common/HTMLWidget", "../chart/MultiChart", "css!./Surface"], factory);
    } else {
        root.layout_Surface = factory(root.d3, root.common_HTMLWidget, root.chart_MultiChart);
    }
}(this, function (d3, HTMLWidget, MultiChart) {
    function Surface() {
        HTMLWidget.call(this);

        this._tag = "div";
    }
    Surface.prototype = Object.create(HTMLWidget.prototype);
    Surface.prototype._class += " layout_Surface";

    Surface.prototype.publish("surfaceTitlePadding", null, "number", "Title Padding (px)",null,{tags:['Basic']});
    Surface.prototype.publish("surfaceTitleFontSize", null, "number", "Title Font Size (px)",null,{tags:['Basic']});
    Surface.prototype.publish("surfaceTitleFontColor", null, "html-color", "Title Font Color",null,{tags:['Basic']});
    Surface.prototype.publish("surfaceTitleFontFamily", null, "string", "Title Font Family",null,{tags:['Basic']});
    Surface.prototype.publish("surfaceTitleFontBold", true, "boolean", "Enable Bold Title Font",null,{tags:['Basic']});
    Surface.prototype.publish("surfaceTitleBackgroundColor", null, "html-color", "Title Background Color",null,{tags:['Basic']});

    Surface.prototype.publish("surfacePadding", null, "string", "Surface Padding (px)", null, { tags: ['Intermediate'] });
    Surface.prototype.publish("surfaceBackgroundColor", null, "html-color", "Surface Background Color",null,{tags:['Basic']});
    Surface.prototype.publish("surfaceBorderWidth", null, "number", "Surface Border Width (px)",null,{tags:['Basic']});
    Surface.prototype.publish("surfaceBorderColor", null, "html-color", "Surface Border Color",null,{tags:['Basic']});
    Surface.prototype.publish("surfaceBorderRadius", null, "number", "Surface Border Radius (px)",null,{tags:['Basic']});

    Surface.prototype.publish("title", "", "string", "Title",null,{tags:['Intermediate']});
    Surface.prototype.publish("surfaceTitleAlignment", "center", "set", "Title Alignment", ["left","right","center"],{tags:['Basic']});
    Surface.prototype.publish("widget", null, "widget", "Widget",null,{tags:['Private']});

    Surface.prototype.testData = function () {
        this.title("ABC");
        this.widget(new Surface().widget(new MultiChart().testData()));
        return this;
    };

    Surface.prototype.widgetSize = function (titleDiv, widgetDiv) {
        var width = this.clientWidth();
        var height = this.clientHeight();
        if (this.title()) {
            height -= this.calcHeight(titleDiv);
        }
        height -= this.calcFrameHeight(widgetDiv);
        width -= this.calcFrameWidth(widgetDiv);
        return { width: width, height: height };
    };

    Surface.prototype.enter = function (domNode, element) {
        HTMLWidget.prototype.enter.apply(this, arguments);
    };

    Surface.prototype.update = function (domNode, element) {
        HTMLWidget.prototype.update.apply(this, arguments);

        element
            .style("border-width",this.surfaceBorderWidth()+'px')
            .style("border-color",this.surfaceBorderColor())
            .style("border-radius",this.surfaceBorderRadius()+'px')
            .style("background-color",this.surfaceBackgroundColor())
        ;

        var titles = element.selectAll(".surfaceTitle").data(this.title() ? [this.title()] : []);
        titles.enter().insert("h3", "div")
            .attr("class", "surfaceTitle")
        ;
        titles
            .text(function (d) { return d; })
            .style("text-align",this.surfaceTitleAlignment())
            .style("color",this.surfaceTitleFontColor())
            .style("font-size",this.surfaceTitleFontSize()+"px")
            .style("font-family",this.surfaceTitleFontFamily())
            .style("font-weight",this.surfaceTitleFontBold() ? "bold" : "normal")
            .style("background-color",this.surfaceTitleBackgroundColor())
            .style("padding",this.surfaceTitlePadding()+"px")
        ;
        titles.exit().remove();

        var widgets = element.selectAll("#" + this._id + " > .surfaceWidget").data(this.widget() ? [this.widget()] : [], function (d) { return d._id; });

        var context = this;
        widgets.enter().append("div")
            .attr("class", "surfaceWidget")
            .each(function (d) {
                d.target(this);
            })
        ;
        widgets
            .style("padding", this.surfacePadding() ? this.surfacePadding() + "px" : null)
            .each(function (d) {
                var widgetSize = context.widgetSize(element.select("h3"), d3.select(this));
                d
                    .resize({ width: widgetSize.width, height: widgetSize.height })
                ;
            })
        ;
        widgets.exit().each(function (d) {
            d.target(null);
        }).remove();
    };

    Surface.prototype.exit = function (domNode, element) {
        if (this.widget()) {
            this.widget(null);
            this.render();
        }
        HTMLWidget.prototype.exit.apply(this, arguments);
    };

    Surface.prototype.render = function (callback) {
        var context = this;
        HTMLWidget.prototype.render.call(this, function (widget) {
            if (context.widget()) {
                context.widget().render(function (widget) {
                    if (callback) {
                        callback(widget);
                    }
                });
            } else {
                if (callback) {
                    callback(widget);
                }
            }
        });
    };

    return Surface;
}));