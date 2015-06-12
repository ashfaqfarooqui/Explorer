'use strict';

var tsControllers = angular.module('tsControllers', []);

  tsControllers.controller('tsController1', function($scope, $http, $timeout) {

    $scope.allViews = {
        transAreClickable: true,
        pollingFrequency: 2000
    };

    $scope.updateProtocol = function(dataFromServer) {
        $scope.protocol = dataFromServer;
    };

    //To set protocol when page is loaded
    $http.get('/api/ts').success(function(dataFromServer)  {
        $scope.updateProtocol(dataFromServer);
    });

    //To poll protocol when page is active
    var poll = function() {
        $timeout(function() {
            $http.get('/api/ts').success(function(dataFromServer)  {
                if(JSON.stringify($scope.protocol.id) != JSON.stringify(dataFromServer.id)) {
                    $scope.updateProtocol(dataFromServer);
                }
                $scope.allViews.transAreClickable = true;
                $scope.checkState();
                poll();
            }).error(function(dataFromServer) {
                console.log("Problem to poll data");
                console.log("dataFromServer: " + dataFromServer)
            });
        }, $scope.allViews.pollingFrequency);
    };
    //Start polling
    poll();

    $scope.clickTransition=function(t){
        $scope.allViews.transAreClickable = false;
        $http.post('/api/ts/transition',t).success(function(dataFromServer)  {
//            $scope.updateProtocol(dataFromServer);
        }).error(function(dataFromServer) {
            console.log("Problem to post transition" + t);
            console.log("dataFromServer: " + dataFromServer);
        });
    };

    //transitionsView--------------------------------------------------------------------------------------
    $scope.transitionsView = {
        filterQuery: "",
        onlyShowEnabled: true
    };

    $scope.transitionsView_filterOnlyShowEnabled = function(t) {
        if($scope.transitionsView.onlyShowEnabled) {
            return t.enabled == "true";
        } else {
            return true;
        }
    };

    $scope.transitionsView_transitionBoxColor = function(obj) {
        var prefix = "list-group-item"
        if(obj.enabled == "true") {
            return prefix + " list-group-item-success"
        }
        return prefix
    };

    //operationsView--------------------------------------------------------------------------------------
    $scope.operationsView = {
        filterQuery: "",
        onlyShowStartable: true,
        onlyShowExecuting: true,
        onlyShowExecutedAtLeastOnce: false
    };

    $scope.operationsView_operationBoxColor = function(obj) {
    var prefix = "list-group-item"
        if(obj.startable == "true") {
            return prefix + " list-group-item-success"
        } else if (obj.executing == "true") {
            return prefix + " active"
        }
        return prefix
    };

    $scope.operationsView_filterOnlyShow = function(obj) {
        if($scope.operationsView.onlyShowExecuting||$scope.operationsView.onlyShowStartable||$scope.operationsView.onlyShowExecutedAtLeastOnce) {
            return ($scope.operationsView.onlyShowExecuting && (obj.executing == "true"))
            ||($scope.operationsView.onlyShowStartable && (obj.startable == "true"))
            ||($scope.operationsView.onlyShowExecutedAtLeastOnce && (parseInt(obj.nbrOfExecutions) > 0));
        } return true;
    };

    //variablesView----------------------------------------------------------------------------------------
    $scope.variablesView = {
        filterQuery: "",
        stateInDropdownIsSafeButtonColor: "btn btn-success disabled",
        stateInDropdownIsSafeButtonText: "Restart from safe state"
    };

    $scope.pushToStateToPost = function(stateToPost) {
        for(var index in $scope.protocol.variables) {
            var obj = $scope.protocol.variables[index]
            stateToPost.push({"name" : obj.name,"value" : obj.setToValue});
        }
    }

    $scope.checkState=function(){
        var stateToPost = [];
        $scope.pushToStateToPost(stateToPost)

        $http.post('/api/ts/stateToCheck',stateToPost).success(function(dataFromServer)  {
//            console.log("dataFromServer stateToCheck: " + dataFromServer);
            if (dataFromServer == "Some(true)") {
                $scope.variablesView.stateInDropdownIsSafeButtonColor = "btn btn-success";
                $scope.variablesView.stateInDropdownIsSafeButtonText = "Restart from safe state";
            } else if(dataFromServer == "Some(false)"){
                $scope.variablesView.stateInDropdownIsSafeButtonColor = "btn btn-danger";
                $scope.variablesView.stateInDropdownIsSafeButtonText = "Restart from unsafe state";
            }
        }).error(function(dataFromServer) {
            console.log("Problem to post state");
            console.log("dataFromServer: " + dataFromServer);
        });
    };

    $scope.clickState=function(){
        $scope.allViews.transAreClickable = false;
        var stateToPost = [];
        $scope.pushToStateToPost(stateToPost)

        $http.post('/api/ts/state',stateToPost).success(function(dataFromServer)  {
//            console.log("clickState " + dataFromServer);
        }).error(function(dataFromServer) {
            console.log("Problem to post state");
            console.log("dataFromServer: " + dataFromServer);
        });
    };

    $scope.variablesView_variableBoxColor = function(obj) {
        var prefix = "list-group-item"
        if(obj.marking == "true") {
            return prefix + " list-group-item-success"
        }
        return prefix
    };

});