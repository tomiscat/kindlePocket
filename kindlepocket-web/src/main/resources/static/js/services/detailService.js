angular.module('detail.service',[])
.factory('detailService', function($q, $http){

    return {
        getDetails : function(){
            var deferred = $q.defer();
            var path = 'http://6bd9895a.ngrok.io/KindlePocket/getDetails';
            var promise = $http.get(path).then(function(response){
                return response;
            }, function(response){
                return response;
            });
            return promise;
        }
    }

});