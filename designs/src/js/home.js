var App = function() {
    function teste(){
        console.log('jquery ok');
    }

    // Menu fixo desktop
    function menuFixed(){
        var nav = $('header'); 
        var child = nav.find(".flex");
        screenW = $(window).width();  
        mHeight = child.height();
        $(window).scroll(function () { 
            if(!$(".menu-mobile").is(":visible"))
            {
                if ($(this).scrollTop() > mHeight) { 
                    nav.addClass("sticky-nav");
                } else { 
                    nav.removeClass("sticky-nav");
                }
            }            
        });  

        $(".menu-mobile").on("click",function(){
            $('header').toggleClass("menu-open");
        });
    }

    function startIcCheckedAnim(){
        TweenMax.to(".ic-service-center",0.5,{scale:1,ease:Back.easeOut,delay:1});
        TweenMax.to(".ic-service-center",0.5,{scale:0,ease:Back.easeIn,delay:4,onComplete:startIcCheckedAnim});
    }

    function startIcCircleAnim(){
        var anim = TweenMax.to(".ic-circle",1,{css:{width:"73%",top:"0%",left:"14%",opacity:0.2},ease:Quart.easeInOut,delay:1,onComplete:function(){
            anim.reverse();
            TweenMax.delayedCall(3, startIcCircleAnim);
        }});
    }

    function setupSlider()
    {
        var contentTabs = $(".content-tabs");
        var tabButtons = $(".check-out .button-mod");
        var prev_bt = contentTabs.find(".owl-prev");
        var next_bt = contentTabs.find(".owl-next");
        var currentTab;
        var tabIndex = 0;

        var Slider = function(tag)
        {
            this.tag = tag;
            var slide_area = tag.find(".slide-tabs");
            var slideContainer = tag.find(".slide-container");
            var total = slideContainer.children().length;
            var currentPage = 0;
            var self = this;
            
            this.open = function(){
                tabButtons.removeClass("active");
                tabButtons.eq(tag.index()).addClass("active");
                currentPage =  0;
                updateNav();
                var complete = function(){                    
                    TweenMax.set(slideContainer,{css:{left:-(currentPage*100)+"%"}});
                    if(currentTab) currentTab.hide();
                    tag.removeClass("hide");
                    TweenMax.to(tag,.5,{alpha:1,ease:Quad.easeInOut});
                    currentTab = self;                    
                };
                if(currentTab)TweenMax.to(currentTab.tag,.5,{alpha:0,ease:Quad.easeInOut,onComplete:complete});
                else complete();
            }
            this.hide = function(){
                tag.addClass("hide");
            }
            this.next = function(){
                currentPage++;
                updateSlider();
            }
            this.prev = function(){
                currentPage--;
                updateSlider();
            }

            function updateSlider(ease){
                ease = ease?ease:Quad.easeInOut;
                currentPage = currentPage<0?0:currentPage;
                currentPage = currentPage>=total?(total-1):currentPage;
                TweenMax.to(slideContainer,.7,{css:{left:-(currentPage*100)+"%"},ease:ease});
                //updateNav();
            }
            /*function updateNav(){
                if(prev_bt.is(":visible") && next_bt.is(":visible")){
                    if(!currentPage){
                        prev_bt.addClass("disable");
                        TweenMax.to(prev_bt,0.75,{alpha:0,ease:Quad.easeInOut});
                    }else{
                        prev_bt.removeClass("disable");
                        TweenMax.to(prev_bt,0.75,{alpha:1,ease:Quad.easeInOut});
                    }
    
                    if(currentPage>=total-1){
                        next_bt.addClass("disable");
                        TweenMax.to(next_bt,0.75,{alpha:0,ease:Quad.easeInOut});
                    }else{
                        next_bt.removeClass("disable");
                        TweenMax.to(next_bt,0.75,{alpha:1,ease:Quad.easeInOut});
                    }
                }                
            }*/
            var _x;
            var _left;
            var value;
            var pct;
            var scrollY;
            function touchStart(e)
            {
                //scrollY = $($("document.body")).scrollTop();
                var touches =  e.originalEvent.touches[0];
                _x = touches.pageX;
                _left = slideContainer[0].style.left;
                _left = _left?Number(_left.replace("%","")):0;
                TweenMax.killTweensOf(slideContainer);
                slide_area.on("touchmove",touchMove);
                $(document).on("touchend",touchEnd);
            }
            function touchMove(e){
                //$("document.body").scrollTop(scrollY);
                var touches =  e.originalEvent.touches[0];
                var dif = (touches.pageX-_x);
                pct = dif/slide_area.width();
                value = _left + (dif/slideContainer.width())*(100*total);              
                TweenMax.set(slideContainer,{css:{left:(value)+"%"}});
                e.preventDefault();
            }
            function touchEnd(e){
                slide_area.off("touchmove",touchMove);
                $(document).off("touchend",touchEnd);
                var dif = (value/100)+currentPage;
                if(Math.abs(pct)>.2) {
                    if(pct>0) currentPage--;
                    else if(pct<0) currentPage++;
                }
                updateSlider(Quad.easeOut);
            }

            if(total>1) slide_area.on("touchstart",touchStart);
        }

        function updateNav(){
            if(prev_bt.is(":visible") && next_bt.is(":visible")){
                if(!tabIndex){
                    prev_bt.addClass("disable");
                    TweenMax.to(prev_bt,0.75,{alpha:0,ease:Quad.easeInOut});
                }else{
                    prev_bt.removeClass("disable");
                    TweenMax.to(prev_bt,0.75,{alpha:1,ease:Quad.easeInOut});
                }

                if(tabIndex>=tabs.length-1){
                    next_bt.addClass("disable");
                    TweenMax.to(next_bt,0.75,{alpha:0,ease:Quad.easeInOut});
                }else{
                    next_bt.removeClass("disable");
                    TweenMax.to(next_bt,0.75,{alpha:1,ease:Quad.easeInOut});
                }
            }                
        }
        function updateTabs(){
            tabIndex = tabIndex<0?0:tabIndex;
            tabIndex = tabIndex>=tabs.length?(tabs.length-1):tabIndex;    
            tabs[tabIndex].open();
        }

        var tabs = [];
        contentTabs.find(".tab-mod").each(function(key, tag) {
            tabs.push(new Slider($(tag)));
        });
        tabs[0].open();

        prev_bt.on("click", function(){
            //currentTab.prev();
            tabIndex--;
            updateTabs();
        });
        next_bt.on("click", function(){
            //currentTab.next();
            tabIndex++;
            updateTabs();
        });
        tabButtons.on("click",function(){
            tabIndex = $(this).index();
            updateTabs();
        });
    }

    function updateFont(){
        var target = $(".banner-topo .content-center");
        var fontSize = target.width()*0.01111111;
        if($(".menu-mobile").is(":visible"))  $("html").css("font-size",fontSize+'px');
        else $("html").css("font-size",'');
    }

    function resize() {
        $(".menu-open").removeClass("menu-open");
        updateFont();
        if($(".menu-mobile").is(":visible")) $('header').addClass("sticky-nav");
        else $('header').removeClass("sticky-nav");

        $("body").addClass("no-transitions");
        TweenMax.killTweensOf("body");
        TweenMax.to("body",.2,{onComplete:function(){            
            $("body").removeClass("no-transitions");
        }});
    }

    function setupForm(){
        var form = $("#newsletter");
        var inputs = form.find(".inputs");
        var input = inputs.find("#email");
        var feedback = form.find(".feedback");

        var message = function(html){
            TweenLite.to(feedback,.3,{alpha:0,ease:Quad.easeInOut,onComplete:function(){
                feedback.html(html);
                TweenLite.to(feedback,.3,{alpha:1,ease:Quad.easeInOut});
            }});
        }

        form.submit(function(e){
            var formData = new FormData(form[0]);
            var val = input.val();
            var re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
            if(!re.test(val.toLowerCase())) {
                message("Please enter a valid email address.");
                inputs.addClass("error");
            }else{
                message("Sending...");
                $.ajax({
                    url: form.attr("action"),
                    type: 'POST',
                    data: formData,
                    success: function(data) {
                        message("Thanks for Joining Us!");
                        inputs.removeClass("error");                            
                    },
                    error:function(){
                        message("Please enter a valid email address.");
                        inputs.addClass("error");
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
            }
            e.preventDefault();
        });
    }

    return {
        init: function() {
            menuFixed();
            setupSlider();
            teste();
            $(".preloader").removeClass("preloader");
            startIcCheckedAnim();
            startIcCircleAnim();
            setupForm();
            resize();
            $(window).resize(resize);
        }
    } 
}();