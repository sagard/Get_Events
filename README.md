Get_Events
==========

RESTful application that takes a event and finds other events on same domain
It takes a event url as input and returns 10 other urls on same domain that contain events.

To run:
==========
You can run the application from http://getevents-basicshopping.rhcloud.com/GetEvents/getactivity.html

or

Run it on your local tomcat by checking out the code and run getactivity.html on a tomcat server

or 
Checkout the code and run Crawler.java.

Logic:

It takes a input event url. 
Read all links from that page and saves in a set 
Examines each link to check if its a event(if it contains exhibition/event/workshop in title,or it contains buy tickets or signup/enroll on page)
It the final Set contains 10 url ,it returns else it goes to root domain and checks all links on the root domain for event pages.
