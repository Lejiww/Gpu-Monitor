# 🤖 GPU & System Monitor with Ollama Controls
A lightweight web-based monitoring solution for GPU/system stats with integrated Ollama service management. Perfect for keeping an eye on your local AI setup!


*Real-time monitoring dashboard with Ollama controls*

## ✨ Features

- 📊 **Real-time GPU monitoring** - Temperature, utilization, VRAM usage, power consumption
- 💻 **System metrics** - CPU usage, RAM, disk space, CPU temperature  
- 🤖 **Ollama service control** - Start, stop, restart Ollama with one click
- 🔄 **Multi-GPU support** - Automatically detects and displays all NVIDIA GPUs
- 🌐 **Web interface** - Clean, responsive dashboard
- ⚡ **Lightweight** - Single JAR file, minimal resource usage
- 🎨 **Color-coded alerts** - Visual warnings for high temperatures and load

## 🖼️ Screenshot

### Main Dashboard
![Main Dashboard](https://ibb.co/KxcphdK8)


## 🚀 Quick Start

### Prerequisites

- **Operating System**: Linux (tested on Ubuntu 20.04/22.04)
- **Java Runtime**: OpenJDK 8+ or Oracle JDK 8+
- **GPU**: NVIDIA GPU with drivers and nvidia-smi installed
- **Ollama**: Ollama installed and configured
- **Utilities**: `screen` (for running in background)

### Installation

1. **Install Java runtime** (if not already installed):
   ```bash
   sudo apt update
   sudo apt install openjdk-11-jre screen
   ```

2. **Download the application**:
   ```bash
   wget https://github.com/Lejiww/Gpu-Monitor/releases/latest/download/gpu-monitor.jar
   ```

3. **Configure sudo permissions** for Ollama control:
   ```bash
   sudo visudo
   ```
   Add this line (replace `your-username` with your actual username):
   ```
   your-username ALL=(ALL) NOPASSWD: /usr/sbin/service ollama start, /usr/sbin/service ollama stop, /usr/sbin/service ollama restart
   ```

4. **Run the application**:
   ```bash
   # Run in screen session to keep it running
   screen -S gpu-monitor
   java -jar gpu-monitor.jar
   
   # Detach from screen: Press Ctrl+A then D
   ```

5. **Access the dashboard**:
   - Local: http://localhost:8080
   - Network: http://YOUR-SERVER-IP:8080

## 📖 Usage

### Starting the Application
```bash
# Start in background using screen
screen -S gpu-monitor
java -jar gpu-monitor.jar

# Detach from screen session
# Press: Ctrl+A, then D
```

### Managing the Application
```bash
# Reattach to the running session
screen -r gpu-monitor

# List all screen sessions
screen -ls

# Stop the application
screen -S gpu-monitor -X quit
```

### Accessing from Network
Replace `YOUR-SERVER-IP` with your actual server IP:
```
http://192.168.1.100:8080
http://10.0.0.50:8080
```

## 🔧 Configuration

### Port Configuration
By default, the application runs on port 8080. To change this, edit the source code and recompile, or use port forwarding:

```bash
# Port forwarding example (if needed)
ssh -L 8080:localhost:8080 user@your-server
```

### Firewall Setup
If accessing from other machines, ensure port 8080 is open:
```bash
# Ubuntu/Debian
sudo ufw allow 8080

# CentOS/RHEL
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

## 🛠️ Development

### Building from Source

1. **Clone the repository**:
   ```bash
   git clone https://github.com/YOUR-USERNAME/gpu-system-monitor.git
   cd gpu-system-monitor
   ```

2. **Compile**:
   ```bash
   javac -d build src/*.java
   ```

3. **Create JAR**:
   ```bash
   cd build
   jar cfe gpu-monitor.jar GPUViewerServer *.class
   ```

4. **Run**:
   ```bash
   java -jar gpu-monitor.jar
   ```

### Project Structure
```
gpu-system-monitor/
├── src/
│   ├── GPUViewerServer.java
│   └── ClientHandler.java
├── docs/
│   └── screenshots/
├── build/
├── README.md
└── LICENSE
```

## 📊 Monitored Metrics

### GPU Metrics (via nvidia-smi)
- GPU name and index
- Temperature (°C)
- Utilization (%)
- Memory usage (MB)
- Power consumption (W)

### System Metrics
- CPU usage (%)
- RAM usage (GB)
- Disk usage (GB)
- CPU temperature (°C)
- Load average
- System uptime
- OS information

## 🤖 Ollama Integration

The application provides seamless integration with Ollama:

- **Service Status**: Real-time status monitoring
- **Start Service**: `sudo service ollama start`
- **Stop Service**: `sudo service ollama stop`
- **Restart Service**: `sudo service ollama restart`

## ⚠️ Troubleshooting

### Common Issues

**Application won't start**
- Ensure Java 8+ is installed: `java -version`
- Check if port 8080 is available: `netstat -tlnp | grep 8080`

**GPU stats not showing**
- Verify nvidia-smi works: `nvidia-smi`
- Check NVIDIA drivers are installed

**Ollama controls not working**
- Verify sudo permissions are configured correctly
- Test manually: `sudo service ollama status`

**Can't access from network**
- Check firewall settings
- Verify the server IP address
- Ensure the application is binding to all interfaces

### Logs and Debugging
The application outputs logs to the console. When running in screen, you can view logs by reattaching to the session:
```bash
screen -r gpu-monitor
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- NVIDIA for the nvidia-smi utility
- Ollama team for the excellent local LLM solution
- Java community for the robust platform

## 📞 Support

If you encounter any issues or have questions:

1. Check the [Issues](https://github.com/YOUR-USERNAME/gpu-system-monitor/issues) page
2. Create a new issue with detailed information
3. Include your system information and error logs

---

**Star ⭐ this repository if you find it useful!**
