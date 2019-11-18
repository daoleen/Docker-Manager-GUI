package me.sunny.generator.docker.controller;



import javafx.fxml.FXML;
import javafx.scene.control.TextArea;


public class HelpController {

    @FXML
    private TextArea txtAreaContent;


    public void init(boolean secure) {
        String text = (secure) ? getSecuredText() : getUnsecuredText();
        txtAreaContent.setText(text);
    }


    private String getSecuredText() {
        return String.format("Docker Daemon: Configure Secure mode\n\n"
                + "\n"
                + "In this section we will configure dockerd to run on remote host in secure mode.\n"
                + "\n"
                + "First of all, connect to remote host using SSH command:\n"
                + "\t\n"
                + "ssh username@host\n"
                + "\n"
                + "Create a folder to store certificates:\n"
                + "\t\n"
                + "sudo mkdir /usr/share/ca-certificates/dockerd\n"
                + "sudo chown appadmin:root /usr/share/ca-certificates/dockerd\n"
                + "\n"
                + "Generate ca-key:\n"
                + "\t\n"
                + "openssl genrsa -aes256 -out ca-key.pem 4096\n"
                + "\n"
                + "Generate x509 certificate (use domain name of your host as a Common Name when asked):\n"
                + "\t\n"
                + "# Common name below should be a server hostname\n"
                + "openssl req -new -x509 -days 3650 -key ca-key.pem -sha256 -out ca.pem\n"
                + "\n"
                + "Generate other required files. Use your domain name instead of ${YOUR_HOSTNAME_HERE}. Use your host IP address instead of ${YOUR_HOST_IP_HERE} (you can know your IP using ping command):\n"
                + "openssl genrsa -out server-key.pem 4096\n"
                + "openssl req -subj \"/CN=${YOUR_HOSTNAME_HERE}\" -sha256 -new -key server-key.pem -out server.csr\n"
                + "echo subjectAltName = DNS:${YOUR_HOSTNAME_HERE},IP:${YOUR_HOST_IP_HERE},IP:127.0.0.1 >> extfile.cnf\n"
                + "echo extendedKeyUsage = serverAuth >> extfile.cnf\n"
                + "openssl x509 -req -days 3650 -sha256 -in server.csr -CA ca.pem -CAkey ca-key.pem -CAcreateserial -out server-cert.pem -extfile extfile.cnf\n"
                + "\n"
                + "Generating client certificates:\n"
                + "openssl genrsa -out key.pem 4096\n"
                + "openssl req -subj '/CN=client' -new -key key.pem -out client.csr\n"
                + "echo extendedKeyUsage = clientAuth > extfile-client.cnf\n"
                + "openssl x509 -req -days 3650 -sha256 -in client.csr -CA ca.pem -CAkey ca-key.pem -CAcreateserial -out cert.pem -extfile extfile-client.cnf\n"
                + "\n"
                + "Remove files which will not be needed anymore:\n"
                + "rm -v client.csr server.csr extfile.cnf extfile-client.cnf\n"
                + "\n"
                + "Set permissions to generated certificates:\n"
                + "chmod -v 0400 ca-key.pem key.pem server-key.pem\n"
                + "chmod -v 0444 ca.pem server-cert.pem cert.pem\n"
                + "\n"
                + "Now you can run dockerd using the command:\n"
                + "dockerd --tlsverify --tlscacert=ca.pem --tlscert=server-cert.pem --tlskey=server-key.pem -H=0.0.0.0:2376\n"
                + "\n"
                + "Let’s edit /lib/systemd/system/docker.service to start dockerd in secure way automatically:\n"
                + "ExecStart=/usr/bin/dockerd --tlsverify --tlscacert=/usr/share/ca-certificates/dockerd/ca.pem --tlscert=/usr/share/ca-certificates/dockerd/server-cert.pem --tlskey=/usr/share/ca-certificates/dockerd/server-key.pem -H tcp://0.0.0.0:2376 -H unix:///var/run/docker.sock --containerd=/run/containerd/containerd.sock\n"
                + "\n"
                + "Restart docker daemon:\n"
                + "sudo service docker stop\n"
                + "sudo systemctl daemon-reload\n"
                + "sudo service docker start\n"
                + "\n"
                + "Open firewall 2376 port. For example:\n"
                + "sudo ufw allow 2376\n"
                + "\n"
                + "Copy ca.pem cert.pem key.pem to client\n");
    }

    private String getUnsecuredText() {
        return String.format("Docker Daemon: Configure Simple (unsecure) mode\n\n"
                + "\n"
                + "In this section we will configure dockerd to run on remote host and listen TCP socket.\n"
                + "\n"
                + "First of all, connect to remote host using SSH command:\n"
                + "ssh username@host\n"
                + "\n"
                + "Edit /lib/systemd/system/docker.service to tell docker to listen tcp port and use local unix socket:\n"
                + "ExecStart=/usr/bin/dockerd -H tcp://0.0.0.0:2375 -H unix:///var/run/docker.sock --containerd=/run/containerd/containerd.sock\n"
                + "\n"
                + "Restart docker daemon:\n"
                + "sudo service docker stop\n"
                + "sudo systemctl daemon-reload\n"
                + "sudo service docker start\n"
                + "\n"
                + "Open firewall 2375 port. For example:\n"
                + "sudo ufw allow 2375\n");
    }
}
