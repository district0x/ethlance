(function(){
    function toggleMenu(){
        if($("body").hasClass("open-menu")) closeMenu();
        else openMenu();
    }
    function openMenu(){
        $("body").addClass("open-menu");
    }
    function closeMenu(){
        $("body").removeClass("open-menu");
        $("header .combobox.open").removeClass("open");
    }

    function toggleFilterTab(){
        if($(".menu-accordion").hasClass("open-filter")) closeFilterTab();
        else openFilterTab();
    }
    function openFilterTab(){
        $(".menu-accordion").addClass("open-filter");
    }
    function closeFilterTab(){
        $(".menu-accordion").removeClass("open-filter");
    }

    function setupMenu(){
        $(".menu-mobile").on("click",toggleMenu);
        $(".menu-accordion .title-aba").on("click",toggleFilterTab);

        /*$(".menu-accordion").on("click",function(e){
            e.stopPropagation();
        });
        $(window).click(function() {
            closeFilterTab();
        });*/
    }

    function updateFont(){
        var target = $(window);
        var fontSize = target.width()*0.01;
        if(!$(".copyright .only-desktop").is(":visible"))  $("html").css("font-size",fontSize+'px');
        else $("html").css("font-size",'');
    }

    /*function setupMenuAccordion(){
        var options = $(".menu-accordion .general-info .opt-title").not(".title-aba");
        options.on("click", function(e){
            var target = $(e.currentTarget);
            var box = target.parent().find(".box-info");

            TweenMax.killTweensOf(box);
            if(!box.hasClass("collapsed")){
                box.addClass("collapsed");
                TweenMax.to(box,.5,{height:0,ease:Quad.easeOut});
            }else{                
                box.removeClass("collapsed");                
                var currentHeight = box.height();                
                box.removeAttr("style");
                box.css("height","auto");
                var height = box.height();
                box.css("height","");
                TweenMax.set(box,{height:currentHeight});
                TweenMax.to(box,.5,{height:height,ease:Quad.easeOut});
            }
        });
    }*/

    var sliders = [];
    var tables = [];

    function setupSlider()
    {
        var Slider = function(tag){
            var self = this;
            var tag = $(tag);
            var container = tag.find(".container");
            var paging = tag.parent().find(".paging");
            var prevBt = paging.find(".prev");
            var nextBt = paging.find(".next");
            var children = container.children();
            var total = children.length;
            var page = 0;
            
            function prev(){
                page--;
                self.update();
            }
            function next(){
                page++;
                self.update();
            }
            this.update = function(){
                var isMobile = $(window).width()<600;
                var _width = isMobile?19.5:30;
                var _margin = isMobile?4:2;

                container.css("width",(total*_width+(total-1)*_margin)+(isMobile?"em":"rem"));
                page = page<0?0:page;
                page = page>=total?total-1:page;
                prevBt.removeClass("disable");
                nextBt.removeClass("disable");
                children.removeClass("active");
                if(!page) prevBt.addClass("disable");
                if(page>=total-1) nextBt.addClass("disable");
                TweenMax.to(container,.5,{css:{"margin-left":-(page*_width+page*_margin)+(isMobile?"em":"rem")},ease:Quad.easeInOut});
                $(children[page]).addClass("active");
            }

            prevBt.on("click",prev);
            nextBt.on("click",next);
            self.update();
        }

        var slider = $(".slider");
        slider.each(function(index,tag){
            sliders.push(new Slider(tag));
        });
    }

    function setupTable(){
        var Table = function(tag){
            var self = this;
            var tag = $(tag);
            var scroll = tag.find(".scroll");
            var container = tag.find(".container");
            var bar = tag.find(".bar");
            var drag = bar.find(".drag");
            var paging = tag.parent().find(".paging");
            var nextBt = paging.find(".next");
            var prevBt = paging.find(".prev");
            var fowardBt = paging.find(".foward");
            var backwardBt = paging.find(".backward");
            var data = container.find(".row").not(".titles").find("div");
            var total = 0;
            var page = 0;

            data.each(function(index,tag){
                var _total = $(tag).children().length;
                total = _total>total?_total:total;
            });

            function show(){
                data.each(function(index,tag){
                    var span = $($(tag).children()[page]);
                    span.addClass("show");
                    TweenMax.set(span,{alpha:0});
                    TweenMax.to(span,.4,{alpha:1,ease:Quad.easeOut});
                });
            }

            function update()
            {
                page = page<0?0:page;
                page = page>=total?total-1:page; 

                var current = data.find(".show");
                if(current[0]){
                    TweenMax.killTweensOf(current);
                    TweenMax.to(current,.4,{alpha:0,ease:Quad.easeOut,onComplete:function(){
                        current.attr("style","");
                        current.removeClass("show");
                        show();
                    }});
                }else show();
                

                if(!page) {
                    prevBt.addClass("disable");
                    backwardBt.addClass("disable");
                }else{
                    prevBt.removeClass("disable");
                    backwardBt.removeClass("disable");
                }

                if(page>=total-1) {
                    nextBt.addClass("disable");
                    fowardBt.addClass("disable");
                }else{
                    nextBt.removeClass("disable");
                    fowardBt.removeClass("disable");
                }
            }

            function updateScroll(){
                if(bar[0] && drag[0])
                {
                    var pct = scroll.scrollLeft()/Math.floor(container.width()-scroll.width());
                    var dragLeft = (bar.width()-drag.width())*pct;
                    TweenMax.set(drag,{css:{left:dragLeft+"px"}});
                }                
            }

            function next(){
                page++;
                update();
            }
            function prev(){
                page--;
                update();
            }
            function foward(){
                page = total-1;
                update();
            }
            function backward(){
                page = 0;
                update();
            }

            nextBt.on("click", next);
            prevBt.on("click", prev);
            fowardBt.on("click", foward);
            backwardBt.on("click", backward);
            scroll.on("scroll",updateScroll);
            update();
        }

        var table = $(".table");
        table.each(function(index,tag){
            tables.push(new Table(tag));
        });
    }

    function setupTabs(){
        var geral = $(".geral-buttons-opts-screen");
        geral.each(function(index,tag){
            var tag = $(tag);
            var tabs_bts = tag.find(".buttons-tabs-screen");
            var select = tag.find(">.combobox >select");
            var tabs = tag.parent().find(".content-tab-screen");
            tabs.addClass("hide");
            var curentTab = tabs.eq(0);
            curentTab.removeClass("hide");
            tabs_bts.on("click",function(e){
                var target = $(e.currentTarget);
                var index = target.index();                
                update(index);                
            });
            select.on("change",function(e){
                update(select[0].selectedIndex);
            });
            var update = function(index){
                var tab = tabs.eq(index);
                tabs_bts.removeClass("active");
                tabs_bts.eq(index).addClass("active");            
                TweenMax.killTweensOf(curentTab);
                TweenMax.to(curentTab,.5,{alpha:0,ease:Quad.easeOut,onComplete:function(){
                    curentTab.addClass("hide");
                    tab.removeClass("hide");
                    TweenMax.set(tab,{alpha:0});
                    TweenMax.to(tab,.5,{alpha:1,ease:Quad.easeOut});
                    curentTab = tab;
                }});
            }
        });
    }

    function setupCombobox(){
        var combos = $(".combobox");
        combos.each(function(i,combo){
            var combo = $(combo);
            var selected = combo.find(".selected");
            var items = combo.find(".items");
            var select = combo.find("select");
            var options = select.find("option");
            options.each(function(j,option){
                var option = $(option);
                var html = option[0].innerHTML;
                var item = $("<div>");
                item.html(html);
                items.append(item);
                item.on("click",function(e){
                    selected.html(e.currentTarget.innerHTML);
                    select[0].selectedIndex = item.index();
                    select.change();
                });          
                if(!j) item.click();
            });
            select.on('mousedown', function(e) {
                combo.removeClass("error");
                if($(window).width()>=600){
                    e.preventDefault();
                    this.blur();
                    window.focus();
                }                
             });
            combo.on("click",function(e){
                if($(window).width()>=600) combo.toggleClass("open");
                e.stopPropagation();
            });
        });
        $(window).click(function() {
            combos.removeClass("open");
        });
    }

    function setupForms(){
        var MAX_FILE_SIZE = 10000000;

        var forms = $(".ajax-form");
        var result = forms.find(".result");

        var message = function(html){
            TweenLite.killTweensOf(result);
            TweenLite.to(result,.3,{alpha:0,ease:Quad.easeInOut,onComplete:function(){
                result.html(html);
                TweenLite.to(result,.3,{alpha:1,ease:Quad.easeInOut});
            }});
        }

        var validade = function(form)
        {
            var getErrorElement = function(element)
            {
                var parent = element.parent();
                return parent;
            }

            var ok = true;

            form.find("input, textarea, select").each(function(index,element){
                element = $(element);
                element.unbind("change paste keydown");
                var val       = element.val();
                var name      = element.attr("name");
                var tagName   = element.prop("tagName").toLowerCase();
                var required  = !(element.attr("ignore")!=undefined);
                var minLength = element.attr("minLength");
                var maxLength = element.attr("maxLength");
                minLength = minLength ? minLength : 2;
                maxLength = maxLength ? maxLength : (tagName=="textarea"?1000:140);
                
                if(!required) {
                    return;
                }

                var _ok = true;

                var type = element.attr("type")
                type = type?type.toLowerCase():type;
                if(type=="email"){
                    var re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
                    if(!re.test(val.toLowerCase())) _ok = false; 
                }else if(type=="checkbox" || type=="radio"){
                    if(!$("input[name='"+name+"']:checked")[0]) _ok = false;
                }else if(type=="file"){
                    if(!val || (val && element[0].files[0].size>MAX_FILE_SIZE)) _ok = false;
                }else if(!val) _ok = false;
                else if(tagName!="select"){
                    var length = val.length;
                    if(length<minLength || length>maxLength) _ok = false;
                }
                
                var erroElement = getErrorElement(element);
                if(!_ok){
                    erroElement.addClass("error");
                    element.bind("change paste keydown",function(e){
                        erroElement.removeClass("error"); 
                        element.unbind("change paste keydown");
                    });
                    ok = _ok;
                }
            });

            return ok;
        }

        function clearForms(form){
            form.find("input");
            form.find(".error").removeClass("error");
            form.find("input, textarea").not("input[type='radio'],input[type='checkbox'],input[type='submit']").val("");
            form.find("input[type='radio']:checked,input[type='checkbox']:checked").prop("checked", false);
            form.find("select").prop("selectedIndex", 0);
            form.removeClass("disable");
        }

        forms.each(function(index,tag){
            var form = $(tag);
            form.submit(function(e){
                var formData = new FormData(form[0]);
                console.log(formData);
                if(validade(form)) {
                    form.addClass("disable");
                    message("Sending...");
                    $.ajax({
                        url: form.attr("action"),
                        type: 'POST',
                        data: formData,
                        success: function(data) {
                            message(data.toString());
                            clearForms(form);
                        },
                        error:function(){
                            message("Please complete missing fields.");
                            form.find(".error").removeClass("error");
                            form.removeClass("disable");
                        },
                        cache: false,
                        contentType: false,
                        processData: false,
                        xhr: function() {
                            var myXhr = $.ajaxSettings.xhr();
                            if (myXhr.upload) {
                                myXhr.upload.addEventListener('progress', function() {
            
                                }, false);
                            }
                            return myXhr;
                        }
                    });
                }else message("Please complete missing fields.");
                e.preventDefault();
            });
        });
    }

    function resize(){
        updateFont();
        for(var i in sliders) sliders[i].update();

        $("body").addClass("no-transitions");
        TweenMax.killTweensOf("body");
        TweenMax.to("body",.2,{onComplete:function(){            
            $("body").removeClass("no-transitions");
        }});
        $(".combobox").removeClass("open");
    }

    setupMenu();
    //setupMenuAccordion();
    setupSlider();
    setupTable();
    setupCombobox();
    setupTabs();
    setupForms();
    resize();
    $(window).on("resize",resize);
})();