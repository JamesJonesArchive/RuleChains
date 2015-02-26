/**
 * Testing AJAX Plugin for handling MessageService Topic & Queue REST Methods via AJAX
 * 
 * Depends on: JQuery
 *  
 */
(function($) {

    $.ruleChains = {
        chainServiceHandler: {
            GETlistChainServiceHandlers: function(json,callback) {
                json = jQuery.extend({
                    name: ""
                },json);
                $.ajax({
                    url: "/RuleChains/chainServiceHandler/",
                    type: "GET",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
//                    data: ($.trim(json.pattern).length < 1)?{}:{
//                        pattern: $.trim(json.pattern)
//                    },
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });        
            },
            PUTaddChainServiceHandler: function(json,callback) {
                json = jQuery.extend(true,{
                    name: ""
                },json);
                $.ajax({
                    url: '/RuleChains/chainServiceHandler/',
                    type: "PUT",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: JSON.stringify(json),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                
            },
            POSTmodifyChainServiceHandler: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    newName: ""
                },json);
                $.ajax({
                    url: '/RuleChains/chainServiceHandler/'+json.name,
                    type: "POST",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: JSON.stringify({
                        chainServiceHandler: json.chainServiceHandler
                    }),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                                
            },
            DELETEdeleteChainServiceHandler: function(json,callback) {
                json = jQuery.extend(true,{
                    name: ""
                },json);
                $.ajax({
                    url: '/RuleChains/chainServiceHandler/'+json.name,
                    type: "DELETE",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    // data: JSON.stringify(json),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                                
            },
            GEThandleChainService: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    input: []
                },json);
                $.ajax({
                    url: '/RuleChains/service/'+json.name+'?'+$.param(json.input),
                    type: "GET",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    // data: JSON.stringify(json),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                
            },
            PUThandleChainService: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    input: []
                },json);
                $.ajax({
                    url: '/RuleChains/service/'+json.name,
                    type: "PUT",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: JSON.stringify({ 
                        input: ("input" in json)?json.input:[]
                    }),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                
            },
            POSThandleChainService: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    input: []
                },json);
                $.ajax({
                    url: '/RuleChains/service/'+json.name,
                    type: "POST",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: JSON.stringify({
                        input: ("input" in json)?json.input:[]
                    }),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                
            },
            DELETEhandleChainService: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    input: []
                },json);
                $.ajax({
                    url: '/RuleChains/service/'+json.name+'?'+$.param(json.input),
                    type: "DELETE",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    // data: JSON.stringify(json),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                                
            }
        },
        config: {
            POSTuploadChainData: function(json_text,callback) {
                $.ajax({
                    url: '/RuleChains/backup/upload/',
                    type: "POST",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: JSON.stringify({
                        upload: json_text
                    }),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                                
            }
        },
        job: {
            GETlistChainJobs: function(json,callback) {
                json = jQuery.extend({
                    name: ""
                },json);
                $.ajax({
                    url: "/RuleChains/job/",
                    type: "GET",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
//                    data: ($.trim(json.pattern).length < 1)?{}:{
//                        pattern: $.trim(json.pattern)
//                    },
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });        
            },
            // listCurrentlyExecutingJobs
            GETlistCurrentlyExecutingJobs: function(json,callback) {
                json = jQuery.extend({
                    name: ""
                },json);
                $.ajax({
                    url: "/RuleChains/running/",
                    type: "GET",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
//                    data: ($.trim(json.pattern).length < 1)?{}:{
//                        pattern: $.trim(json.pattern)
//                    },
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });        
            },
            POSTmergescheduleChainJob: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    mergeName: ""
                },json);
                $.ajax({
                    url: '/RuleChains/job/',
                    type: "POST",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: JSON.stringify({
                        name: json.name,
                        mergeName: json.mergeName
                    }),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                                
            },
            PUTcreateChainJob: function(json,callback) {
                json = jQuery.extend(true,{
                    name: ""
                },json);
                $.ajax({
                    url: '/RuleChains/job/'+json.name,
                    type: "PUT",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: JSON.stringify({ 
                        cronExpression: json.cronExpression,
                        input: ("input" in json)?json.input:[],
                        emailLog: ("emailLog" in json)?json.emailLog:""
                    }),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                
            },
            POSTupdateChainJob: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    newName: ""
                },json);
                $.ajax({
                    url: '/RuleChains/job/'+json.name,
                    type: "POST",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: JSON.stringify({
                        newName: json.newName
                    }),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                
            },
            POSTupdateChainJobEmailLog: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    emailLog: ""
                },json);
                $.ajax({
                    url: '/RuleChains/job/email/'+json.name,
                    type: "POST",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: JSON.stringify({
                        emailLog: json.emailLog
                    }),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                
            },
            DELETEremoveChainJob: function(json,callback) {
                json = jQuery.extend(true,{
                    name: ""
                },json);
                $.ajax({
                    url: '/RuleChains/job/'+json.name,
                    type: "DELETE",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    // data: JSON.stringify(json),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                                
            },
            DELETEunscheduleChainJob: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    cronExpression: ""
                },json);
                $.ajax({
                    url: '/RuleChains/job/'+json.name+'/'+encodeURIComponent(json.cronExpression),
                    type: "DELETE",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    // data: JSON.stringify(json),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                                
            },
            POSTrescheduleChainJob: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    cronExpression: "",
                    cron: ""
                },json);
                $.ajax({
                    url: '/RuleChains/job/'+json.name+'/'+encodeURIComponent(json.cronExpression),
                    type: "POST",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: JSON.stringify({
                        cron: json.cron
                    }),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                                
            },
            PUTaddscheduleChainJob: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    cronExpression: ""
                },json);
                $.ajax({
                    url: '/RuleChains/job/'+json.name+'/'+encodeURIComponent(json.cronExpression),
                    type: "PUT",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
//                    data: JSON.stringify({ 
//                        cronExpression: json.cronExpression,
//                        input: ("input" in json)?json.input:[]
//                    }),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                
            },
            GETgetJobLogs: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    records: 10,
                    offset: 1
                },json);
                $.ajax({
                    url: '/RuleChains/history/'+json.name,
                    type: "GET",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: {
                        records: $.trim(json.records),
                        offset: $.trim(json.offset)
                    },
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                                
            },
            GETgetJobHistories: function(json,callback) {
                json = jQuery.extend({
                    name: ""
                },json);
                $.ajax({
                    url: "/RuleChains/history/",
                    type: "GET",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
//                    data: ($.trim(json.pattern).length < 1)?{}:{
//                        pattern: $.trim(json.pattern)
//                    },
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                        
            },
            DELETEdeleteJobHistory: function(json,callback) {
                json = jQuery.extend(true,{
                    name: ""
                },json);
                $.ajax({
                    url: '/RuleChains/history/'+json.name,
                    type: "DELETE",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    // data: JSON.stringify(json),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                                
            },
            GETgetJobRuleTimings: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    records: 10,
                    offset: 1
                },json);
                $.ajax({
                    url: '/RuleChains/timing/'+json.name,
                    type: "GET",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: {
                        records: $.trim(json.records),
                        offset: $.trim(json.offset)
                    },
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                                                
            }
        },
        chain: {
            GETlistChains: function(json,callback) {
                json = jQuery.extend({
                    pattern: ""
                },json);
                $.ajax({
                    url: "/RuleChains/chain/",
                    type: "GET",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: ($.trim(json.pattern).length < 1)?{}:{
                        pattern: $.trim(json.pattern)
                    },
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });        
            },
            PUTaddChain: function(json,callback) {
                json = jQuery.extend(true,{
                    name: ""
                },json);
                $.ajax({
                    url: '/RuleChains/chain/',
                    type: "PUT",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: JSON.stringify(json),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                
            },
            POSTmodifyChain: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    chain: {
                        name: ""
                    }
                },json);
                $.ajax({
                    url: '/RuleChains/chain/'+json.name,
                    type: "POST",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: JSON.stringify(json),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                
            },
            DELETEdeleteChain: function(json,callback) {
                json = jQuery.extend(true,{
                    name: ""
                },json);
                $.ajax({
                    url: '/RuleChains/chain/'+json.name,
                    type: "DELETE",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    // data: JSON.stringify(json),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                
            },
            GETgetChain: function(json,callback) {
                json = jQuery.extend(true,{
                    name: ""
                },json);
                $.ajax({
                    url: '/RuleChains/chain/'+json.name,
                    type: "GET",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    // data: JSON.stringify(json),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                
            },
            PUTaddChainLink: function(json,callback) {
                json = jQuery.extend(true,{
                    name: ""
                },json);
                $.ajax({
                    url: '/RuleChains/chain/'+json.name,
                    type: "PUT",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: JSON.stringify({ link: json.link }),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                
            },
            GETgetSources: function(json,callback) {
                json = jQuery.extend({
                    pattern: ""
                },json);
                $.ajax({
                    url: "/RuleChains/source/",
                    type: "GET",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
//                    data: ($.trim(json.pattern).length < 1)?{}:{
//                        pattern: $.trim(json.pattern)
//                    },
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });        
            },
            DELETEdeleteChainLink: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    sequenceNumber: 1
                },json);
                $.ajax({
                    url: '/RuleChains/chain/'+json.name+'/'+json.sequenceNumber,
                    type: "DELETE",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    // data: JSON.stringify(json),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                
            },
            POSTmodifyChainLink: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    link: {
                        sequenceNumber: 1
                    }
                },json);
                $.ajax({
                    url: '/RuleChains/chain/'+json.name+'/'+json.link.sequenceNumber,
                    type: "POST",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: JSON.stringify({ link:json.link }),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                                
            },
            POSTmoveChainLink: function(json,callback) {
                json = jQuery.extend(true,{
                    originalChainName: "",
                    destinationChainName: "",
                    originalSequenceNumber: 1,
                    destinationSequenceNumber: 2
                },json);
                $.ajax({
                    url: '/RuleChains/chain/',
                    type: "POST",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: JSON.stringify(json),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                                
            }
        },
        ruleSet: {
            GETlistRuleSets: function(json,callback) {
                json = jQuery.extend({
                    pattern: ""
                },json);
                $.ajax({
                    url: "/RuleChains/ruleSet/",
                    type: "GET",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: ($.trim(json.pattern).length < 1)?{}:{
                        pattern: $.trim(json.pattern)
                    },
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });
            },
            PUTaddRuleSet: function(json,callback) {
                json = jQuery.extend(true,{
                    name: ""
                },json);
                $.ajax({
                    url: '/RuleChains/ruleSet/',
                    type: "PUT",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: JSON.stringify(json),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                

            },
            GETgetRuleSet: function(json,callback) {
                json = jQuery.extend(true,{
                    name: ""
                },json);
                $.ajax({
                    url: '/RuleChains/ruleSet/'+json.name,
                    type: "GET",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    // data: JSON.stringify(json),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                
            },
            DELETEdeleteRuleSet: function(json,callback) {
                json = jQuery.extend(true,{
                    name: ""
                },json);
                $.ajax({
                    url: '/RuleChains/ruleSet/'+json.name,
                    type: "DELETE",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    // data: JSON.stringify(json),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                
            },
            POSTmodifyRuleSet: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    ruleSet: {
                        name: ""
                    }
                },json);
                $.ajax({
                    url: '/RuleChains/ruleSet/'+json.name,
                    type: "POST",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: JSON.stringify(json),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                
            },
            GETgetRule: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    ruleSetName: ""
                },json);
                $.ajax({
                    url: '/RuleChains/ruleSet/'+json.ruleSetName.trim()+'/'+json.name.trim(),
                    type: "GET",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    // data: JSON.stringify(json),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                                
            },
            PUTaddRule: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    ruleSetName: "",
                    serviceType: ""
                },json);
                $.ajax({
                    url: '/RuleChains/ruleSet/'+json.ruleSetName.trim()+'/'+json.name.trim(),
                    type: "PUT",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: JSON.stringify({ serviceType: json.serviceType }),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                
                
            },
            POSTupdateRule: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    ruleSetName: "",
                    rule: {
                        
                    }
                },json);
                $.ajax({
                    url: '/RuleChains/ruleSet/'+json.ruleSetName.trim()+'/'+json.name.trim(),
                    type: "POST",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
                    data: JSON.stringify({ 
                        rule: json.rule 
                    }),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                
            },
            POSTupdateRuleName: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    ruleSetName: "",
                    newName: ""
                },json);
                $.ajax({
                    url: '/RuleChains/ruleSet/'+json.ruleSetName.trim()+'/'+json.name.trim()+'/'+json.newName.trim(),
                    type: "POST",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
//                    data: JSON.stringify({ 
//                        rule: json.rule 
//                    }),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                
            },
            DELETEdeleteRule: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    ruleSetName: ""
                },json);
                $.ajax({
                    url: '/RuleChains/ruleSet/'+json.ruleSetName.trim()+'/'+json.name.trim(),
                    type: "DELETE",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
//                    data: JSON.stringify({ 
//                        rule: json.rule 
//                    }),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                
            },
            PUTmoveRule: function(json,callback) {
                json = jQuery.extend(true,{
                    name: "",
                    ruleSetName: "",
                    newName: ""
                },json);
                $.ajax({
                    url: '/RuleChains/ruleSet/'+json.ruleSetName.trim()+'/'+json.name.trim()+'/'+json.newName.trim(),
                    type: "PUT",
                    dataType : "json",
                    beforeSend: function (XMLHttpRequest, settings) {
                        XMLHttpRequest.setRequestHeader("Content-Type", "application/json");
                        XMLHttpRequest.setRequestHeader("Accept", "application/json");
                    },
//                    data: JSON.stringify({ 
//                        rule: json.rule 
//                    }),
                    success: callback,
                    error: function (jqXHR,  textStatus, errorThrown) {
                        if (jqXHR.status === 0) {
                            // Session has probably expired and needs to reload and let CAS take care of the rest
                            alert('Your session has expired, the page will need to reload and you may be asked to log back in');
                            // reload entire page - this leads to login page
                            window.location.reload();
                        }
                    }
                });                                
            }
            
        }
    };
})(jQuery);       



