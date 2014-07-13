mnstrspeed/seamless
========

Ports
-----
* Instance Server
 * ``INSTANCESERVER_LOCAL_PORT = 1901``
 * ``INSTANCESERVER_DISCOVERY_PORT = 1902``
 * ``INSTANCESERVER_REMOTE_PORT = 1903``
* Package Manager
 * ``PACKAGEMANAGER_DISCOVERY_PORT = 1811``
 * ``PACKAGEMANAGER_PORT = 1812``

Installation
------------
* ``git clone https://github.com/mnstrspeed/seamless.git``
* ``mkdir /opt/seamless``
* ``ln -s seamless/bin/instanceserver*.jar /opt/seamless/instanceserver.jar``

### systemd
/etc/systemd/system/instanceserver.service
```
[Unit]
Name=instanceserver
Description=Seamless Instance Server
After=network.target

[Service]
Type=simple
User=root
StandardOutput=journal
StandardError=journal
WorkingDirectory=/opt/seamless
Environment=CLASSPATH=/opt/seamless/instanceserver.jar
ExecStart=/usr/bin/java nl.tomsanders.seamless.instanceserver.Launcher
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

* ``systemctl enable instanceserver``
* ``reboot``

### Upstart
/etc/init/instanceserver.conf
```
start on startup

script
	chdir /opt/seamless
	exec /usr/bin/java -cp instanceserver.jar nl.tomsanders.seamless.instanceserver.Launcher >> /var/log/instanceserver.sys.log
end script
```

``initctl start instanceserver``
