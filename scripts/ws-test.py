#!/usr/bin/python
# -*- coding: utf-8 -*-


import websocket
import thread
import time

from transit.writer import Writer
from transit.reader import Reader
from StringIO import StringIO

io = StringIO()
writer = Writer(io, "json")
def on_message(ws, message):
    print "message received: ", message

def on_error(ws, error):
    print "error ", error

def on_close(ws):
    print "### closed ###"

def on_open(ws):
    def run(*args):
        writer.write({"command": "init", "token": "token-1"})
        s = io.getvalue()
        s = '["^ ","~:command","~:refresh","~:token","eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjU3YWIwNjkwMGIxOTFkNTVjNjBlZWY2MyIsImV4cCI6MTQ3MDkxMjU1NSwiaWF0IjoxNDcwODI2MTU1fQ.AwI_JLjVVEZz0hlCnu5ahkbs9_L6rWk-_sBZeb1D_cl_UWikEMq2Nz5TugDXErIVVsAMOIQ3nwO39TYnQk5E6fwghve3L9yTpY_-VHUs7q7D6Q-leQoKHRjqYb8XwhIASzhRRRIN4VroLOblpzQcLhzBmH4lY_M4Zd8lxQH--28"]'
        #s = 'äöü'
        print "str ",s
        ws.send(s)
        
        time.sleep(10)
        ws.close()
        print "thread terminating..."
    thread.start_new_thread(run, ())


if __name__ == "__main__":
    websocket.enableTrace(True)
    #ws = websocket.WebSocketApp("ws://echo.websocket.org/",
    ws = websocket.WebSocketApp("ws://localhost:3000/ws",
    #ws = websocket.WebSocketApp("ws://e02.kangrd.com/ws",
                                on_message = on_message,
                                on_error = on_error,
                                on_close = on_close)
    ws.on_open = on_open

    ws.run_forever()
