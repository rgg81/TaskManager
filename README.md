# Task Manager
With Task Manager we refer to a software component that is designed for handling multiple processes inside an operating system. Each process is identified by 2 fields, a unique unmodifiable identifier (PID), and a priority (low, medium, high). The process is immutable, it is generated with a priority and will die with this priority – each process has a kill() method that will destroy it
Task Manager exposes the following
functionality: Add a process, List running processes and Kill/KillGroup/KillAl

## Install

- Requires java 17 and maven 3.8

Install and add dependency to your project:   
`$ mvn install` 

```xml
<dependency>   
<groupId>me.roberto</groupId>   
<artifactId>taskManager</artifactId>   
<version>1.0-SNAPSHOT</version>    
<scope>compile</scope>    
</dependency>
```
## Documentation
Please refer to the test cases to outline all scenarios.
