seamless
========

ports
-----
* PackageManager:
  * ``DISCOVERY_PORT = 1811``
  * ``PACKAGEMANAGER_PORT = 1812``
* InstanceServer:
  * ``DISCOVERY_PORT = ?``
  * ``INSTANCESERVER_INTERNAL_PORT = ?``
  * ``INSTANCESERVER_EXTERNAL_PORT = ?``

Installation
------------
* ``git clone https://github.com/mnstrspeed/seamless.git``
* ``mkdir /opt/seamless``
* ``ln -s seamless/bin/instanceserver*.jar /opt/seamless/instanceserver.jar``
* ``ln -s seamless/bin/packagemanager*.java /opt/seamless/packagemanager.jar``
* ``mkdir /opt/seamless/packages``

/etc/systemd/system/packagemanager.service
```
[Unit]
Name=packagemanager
Description=Seamless Package Manager Service
After=network.target

[Service]
Type=simple
User=root
StandardOutput=journal
StandardError=journal
WorkingDirectory=/opt/seamless
Environment=CLASSPATH=/opt/seamless/packagemanager.jar
ExecStart=/opt/java-current/bin/java nl.tomsanders.seamless.packagemanager.Launcher start -verbose
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```
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
ExecStart=/opt/java-current/bin/java nl.tomsanders.seamless.instanceserver.Launcher
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

* ``systemctl enable instanceserver``
* ``systemctl enable packagemanager``
* ``reboot``
