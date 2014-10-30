var http = require('http')
/*
var server = http.createServer(function(req, resp) {
    resp.writeHead(200, {'content-type': 'text/plain'})
    resp.write('Hello\n')
    setTimeout(function() {
        resp.end('World\n')
    }, 3000);
});
server.listen(8000)
*/

var server = http.createServer(function(req,resp) {
        console.log('Server')
        var body = '';
        console.log('Response')

        req.on('data', function (chunk) {
             body += chunk;
             console.log('DATA: ' + chunk.length)
        })
     
        req.on('end', function () {
             console.log('END: ' + body.length)
             resp.end()
        })
})
server.listen(8000)
