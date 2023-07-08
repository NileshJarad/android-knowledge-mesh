# Consistent Hashing

---


## Hashing
A hash function maps a key to specific node. In a distributed system, a hash function is used to get the server/node info with a key. 
This works perfectly well, for a system with defined set of nodes/ servers. Usually, this is done by the load balancers.

The problem is with increase of requests and if at all a new server needs to be added, without disturbing all requests. 

i.e. A load balancer which can handle dynamic distributed system.

how to design the load balancer?  

--- 

### Consistent Hashing

A technique used to efficiently distribute and balance data across multiple servers/nodes. Also, when the number of nodes change, this minimises the amount of data movement. 

Imagine you have a bunch of data that you want to store on multiple computers. Traditionally, you would use a hash function to determine which computer should store each piece of data. However, this approach becomes problematic when you add or remove computers from the system. It requires moving a lot of data around, which can be slow and inefficient.

Consistent hashing solves this problem by using a clever technique. Instead of directly mapping data to specific computers, it creates a ring-like structure where the computers are positioned. Each computer is assigned a position on the ring based on its identifier or address.



