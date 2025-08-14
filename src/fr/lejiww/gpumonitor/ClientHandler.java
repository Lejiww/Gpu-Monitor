package fr.lejiww.gpumonitor;

import java.io.*;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        System.out.println("=== NEW CLIENT CONNECTION: " + socket.getRemoteSocketAddress() + " ===");
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

            String requestLine = in.readLine();
            if (requestLine == null) return;
            
            System.out.println("=== REQUEST: " + requestLine + " ===");

            if (requestLine.startsWith("GET /update")) {
                handleUpdateRequest(out);
            } else if (requestLine.startsWith("GET /system")) {
                handleSystemRequest(out);
            } else if (requestLine.startsWith("GET /debug")) {
                handleDebugPageRequest(out);
            } else if (requestLine.startsWith("GET /ollama/start")) {
                handleOllamaAction(out, "start");
            } else if (requestLine.startsWith("GET /ollama/stop")) {
                handleOllamaAction(out, "stop");
            } else if (requestLine.startsWith("GET /ollama/restart")) {
                handleOllamaAction(out, "restart");
            } else if (requestLine.startsWith("GET /ollama/status")) {
                handleOllamaStatus(out);
            } else {
                handleRootRequest(out);
            }

        } catch (IOException e) {
            System.err.println("Error handling request: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleOllamaAction(BufferedWriter out, String action) throws IOException {
        try {
            ProcessBuilder pb = new ProcessBuilder("sudo", "service", "ollama", action);
            Process process = pb.start();
            process.waitFor();
            
            String response = "{\"success\": true, \"action\": \"" + action + "\"}";
            out.write("HTTP/1.1 200 OK\r\n");
            out.write("Content-Type: application/json; charset=UTF-8\r\n");
            out.write("Access-Control-Allow-Origin: *\r\n");
            out.write("Content-Length: " + response.length() + "\r\n");
            out.write("\r\n");
            out.write(response);
            out.flush();
        } catch (Exception e) {
            String response = "{\"success\": false, \"action\": \"" + action + "\"}";
            out.write("HTTP/1.1 500 Internal Server Error\r\n");
            out.write("Content-Type: application/json; charset=UTF-8\r\n");
            out.write("Access-Control-Allow-Origin: *\r\n");
            out.write("Content-Length: " + response.length() + "\r\n");
            out.write("\r\n");
            out.write(response);
            out.flush();
        }
    }

    private void handleOllamaStatus(BufferedWriter out) throws IOException {
        try {
            ProcessBuilder pb = new ProcessBuilder("systemctl", "is-active", "ollama");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String status = reader.readLine();
            reader.close();
            process.waitFor();
            
            if (status == null) status = "unknown";
            
            out.write("HTTP/1.1 200 OK\r\n");
            out.write("Content-Type: text/plain; charset=UTF-8\r\n");
            out.write("Access-Control-Allow-Origin: *\r\n");
            out.write("Content-Length: " + status.length() + "\r\n");
            out.write("\r\n");
            out.write(status);
            out.flush();
        } catch (Exception e) {
            String status = "unknown";
            out.write("HTTP/1.1 200 OK\r\n");
            out.write("Content-Type: text/plain; charset=UTF-8\r\n");
            out.write("Access-Control-Allow-Origin: *\r\n");
            out.write("Content-Length: " + status.length() + "\r\n");
            out.write("\r\n");
            out.write(status);
            out.flush();
        }
    }

    private void handleRootRequest(BufferedWriter out) throws IOException {
        String html = generateHtmlPage();
        out.write("HTTP/1.1 200 OK\r\n");
        out.write("Content-Type: text/html; charset=UTF-8\r\n");
        out.write("Content-Length: " + html.length() + "\r\n");
        out.write("\r\n");
        out.write(html);
        out.flush();
    }

    private void handleUpdateRequest(BufferedWriter out) throws IOException {
        try {
            String gpuInfo = executeNvidiaSmiCommand();
            
            out.write("HTTP/1.1 200 OK\r\n");
            out.write("Content-Type: text/plain; charset=UTF-8\r\n");
            out.write("Access-Control-Allow-Origin: *\r\n");
            out.write("Content-Length: " + gpuInfo.length() + "\r\n");
            out.write("\r\n");
            out.write(gpuInfo);
            out.flush();
            
        } catch (Exception e) {
            String errorMsg = "GPU Not Available,0,0,0,0,0,0";
            out.write("HTTP/1.1 200 OK\r\n");
            out.write("Content-Type: text/plain; charset=UTF-8\r\n");
            out.write("Access-Control-Allow-Origin: *\r\n");
            out.write("Content-Length: " + errorMsg.length() + "\r\n");
            out.write("\r\n");
            out.write(errorMsg);
            out.flush();
        }
    }

    private void handleSystemRequest(BufferedWriter out) throws IOException {
        try {
            String systemInfo = getSystemInfo();
            
            out.write("HTTP/1.1 200 OK\r\n");
            out.write("Content-Type: application/json; charset=UTF-8\r\n");
            out.write("Access-Control-Allow-Origin: *\r\n");
            out.write("Content-Length: " + systemInfo.length() + "\r\n");
            out.write("\r\n");
            out.write(systemInfo);
            out.flush();
            
        } catch (Exception e) {
            String fallbackJson = "{\"cpuUsage\":0.0,\"ramUsed\":\"0\",\"ramTotal\":\"0\",\"ramPercentage\":0,\"diskUsed\":\"0\",\"diskTotal\":\"0\",\"diskPercentage\":0,\"cpuTemp\":0,\"loadAvg\":\"0.00 0.00 0.00\",\"osInfo\":\"Unknown\",\"hostname\":\"Unknown\",\"uptime\":\"Unknown\",\"cpuCores\":0}";
            
            out.write("HTTP/1.1 200 OK\r\n");
            out.write("Content-Type: application/json; charset=UTF-8\r\n");
            out.write("Access-Control-Allow-Origin: *\r\n");
            out.write("Content-Length: " + fallbackJson.length() + "\r\n");
            out.write("\r\n");
            out.write(fallbackJson);
            out.flush();
        }
    }

    private String executeNvidiaSmiCommand() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "nvidia-smi",
                "--query-gpu=name,temperature.gpu,utilization.gpu,memory.used,memory.total,power.draw,power.limit",
                "--format=csv,noheader,nounits"
            );
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();
            
            int exitCode = process.waitFor();
            
            if (line != null && !line.trim().isEmpty() && exitCode == 0) {
                return line.trim().replaceAll("\\s*,\\s*", ",");
            }
        } catch (Exception e) {
        }
        return "GPU Not Available,0,0,0,0,0,0";
    }

    private String getSystemInfo() {
    	double cpuUsage = 0;
    	try {
    	    ProcessBuilder pb = new ProcessBuilder("top", "-bn1");
    	    Process process = pb.start();
    	    
    	    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    	    String line;
    	    while ((line = reader.readLine()) != null) {
    	        if (line.contains("Cpu(s)")) {
    	            Pattern pattern = Pattern.compile("([0-9.]+)\\s+id");
    	            Matcher matcher = pattern.matcher(line);
    	            if (matcher.find()) {
    	                double idle = Double.parseDouble(matcher.group(1));
    	                cpuUsage = Math.max(0, 100 - idle);
    	                break;
    	            }
    	        }
    	    }
    	    reader.close();
    	    process.waitFor();
    	} catch (Exception e) {
    	    cpuUsage = 0;
    	}
        
        double ramUsed = 0, ramTotal = 0, ramPercentage = 0;
        try {
            ProcessBuilder pb = new ProcessBuilder("free", "-m");
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            reader.readLine();
            String memLine = reader.readLine();
            reader.close();
            
            int exitCode = process.waitFor();
            
            if (memLine != null && exitCode == 0) {
                String[] parts = memLine.trim().split("\\s+");
                if (parts.length >= 3) {
                    long total = Long.parseLong(parts[1]);
                    long used = Long.parseLong(parts[2]);
                    ramTotal = total / 1024.0;
                    ramUsed = used / 1024.0;
                    ramPercentage = (double) used / total * 100;
                }
            }
        } catch (Exception e) {
            ramUsed = 0;
            ramTotal = 0;
            ramPercentage = 0;
        }
        
        double diskUsed = 0, diskTotal = 0, diskPercentage = 0;
        try {
            File root = new File("/");
            long total = root.getTotalSpace();
            long free = root.getFreeSpace();
            long used = total - free;
            
            if (total > 0) {
                diskTotal = total / (1024.0 * 1024.0 * 1024.0);
                diskUsed = used / (1024.0 * 1024.0 * 1024.0);
                diskPercentage = (double) used / total * 100;
            }
        } catch (Exception e) {
            diskUsed = 0;
            diskTotal = 0;
            diskPercentage = 0;
        }
        
        double cpuTemp = 0;
        try {
            String[] paths = {
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp"
            };
            
            for (String path : paths) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(path));
                    String temp = reader.readLine();
                    reader.close();
                    if (temp != null) {
                        double tempValue = Double.parseDouble(temp.trim()) / 1000.0;
                        if (tempValue > 0 && tempValue < 150) {
                            cpuTemp = tempValue;
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            cpuTemp = 0;
        }
        
        String osInfo = "Unknown";
        String hostname = "Unknown";
        String uptime = "Unknown";
        int cpuCores = 0;
        String loadAvg = "0.00 0.00 0.00";
        
        try {
            cpuCores = Runtime.getRuntime().availableProcessors();
        } catch (Exception e) {
            cpuCores = 0;
        }
        
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/etc/hostname"));
            String h = reader.readLine();
            if (h != null && !h.trim().isEmpty()) {
                hostname = h.trim();
            }
            reader.close();
        } catch (Exception e) {
            hostname = "Unknown";
        }
        
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/etc/os-release"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("PRETTY_NAME=")) {
                    osInfo = line.substring(13).replace("\"", "");
                    break;
                }
            }
            reader.close();
        } catch (Exception e) {
            osInfo = "Unknown";
        }
        
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/uptime"));
            String line = reader.readLine();
            reader.close();
            if (line != null) {
                double uptimeSeconds = Double.parseDouble(line.split(" ")[0]);
                long hours = (long) (uptimeSeconds / 3600);
                long minutes = (long) ((uptimeSeconds % 3600) / 60);
                uptime = (hours > 0 ? hours + "h " : "") + minutes + "m";
            }
        } catch (Exception e) {
            uptime = "Unknown";
        }
        
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/loadavg"));
            String line = reader.readLine();
            reader.close();
            if (line != null) {
                String[] parts = line.split(" ");
                if (parts.length >= 3) {
                    loadAvg = parts[0] + " " + parts[1] + " " + parts[2];
                }
            }
        } catch (Exception e) {
            loadAvg = "0.00 0.00 0.00";
        }
        
        return String.format(
            "{" +
                "\"cpuUsage\":%.1f," +
                "\"ramUsed\":\"%.1f\"," +
                "\"ramTotal\":\"%.1f\"," +
                "\"ramPercentage\":%.1f," +
                "\"diskUsed\":\"%.0f\"," +
                "\"diskTotal\":\"%.0f\"," +
                "\"diskPercentage\":%.1f," +
                "\"cpuTemp\":%.0f," +
                "\"loadAvg\":\"%s\"," +
                "\"osInfo\":\"%s\"," +
                "\"hostname\":\"%s\"," +
                "\"uptime\":\"%s\"," +
                "\"cpuCores\":%d" +
            "}",
            cpuUsage, ramUsed, ramTotal, ramPercentage,
            diskUsed, diskTotal, diskPercentage, cpuTemp,
            loadAvg, osInfo, hostname, uptime, cpuCores
        );
    }

    private String generateHtmlPage() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"fr\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>GPU & System Monitor</title>\n" +
                "  <style>\n" +
                "    body { margin: 0; font-family: 'Segoe UI', sans-serif; background: #f4f6f9; color: #333; display: flex; flex-direction: column; min-height: 100vh; }\n" +
                "    header { background: linear-gradient(135deg, #2c3e50, #34495e); color: white; padding: 20px; text-align: center; font-size: 24px; font-weight: bold; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n" +
                "    main { flex: 1; display: flex; padding: 20px; gap: 20px; }\n" +
                "    .sidebar { flex: 1; max-width: 280px; display: flex; flex-direction: column; gap: 15px; height: fit-content; }\n" +
                "    .info-card, .settings-card, .system-card, .ollama-card { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }\n" +
                "    .info-card h3, .settings-card h3, .system-card h3, .ollama-card h3 { margin: 0 0 15px 0; font-size: 16px; color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 8px; }\n" +
                "    .info-item { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 13px; }\n" +
                "    .info-label { color: #7f8c8d; font-weight: 500; }\n" +
                "    .info-value { font-weight: 600; text-align: right; color: #2c3e50; }\n" +
                "    .metric { margin-bottom: 10px; padding: 8px 10px; background: #f8f9fa; border-radius: 6px; border-left: 3px solid #3498db; }\n" +
                "    .metric-label { font-weight: 600; color: #2c3e50; font-size: 12px; }\n" +
                "    .metric-value { font-size: 14px; color: #27ae60; margin-top: 2px; }\n" +
                "    .progress-bar { width: 100%; height: 6px; background: #e0e0e0; border-radius: 3px; overflow: hidden; margin-top: 6px; }\n" +
                "    .progress-fill { height: 100%; background: linear-gradient(90deg, #27ae60, #2ecc71); transition: width 0.3s ease; }\n" +
                "    .ollama-btn { width: 100%; padding: 10px; margin: 5px 0; border: none; border-radius: 6px; cursor: pointer; font-weight: bold; }\n" +
                "    .btn-start { background: #27ae60; color: white; }\n" +
                "    .btn-stop { background: #e74c3c; color: white; }\n" +
                "    .btn-restart { background: #f39c12; color: white; }\n" +
                "    .ollama-status { margin-bottom: 10px; padding: 5px; background: #f8f9fa; border-radius: 4px; font-size: 12px; }\n" +
                "    .content { flex: 3; background: #fff; border-radius: 12px; padding: 25px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); overflow-y: auto; }\n" +
                "    .content h2 { margin-top: 0; font-size: 22px; margin-bottom: 20px; color: #2c3e50; }\n" +
                "    table { width: 100%; border-collapse: collapse; margin-bottom: 20px; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }\n" +
                "    th, td { padding: 15px 12px; text-align: left; border-bottom: 1px solid #e0e0e0; }\n" +
                "    th { background: linear-gradient(135deg, #2c3e50, #34495e); color: white; font-weight: 600; font-size: 14px; }\n" +
                "    tr:nth-child(even) { background-color: #f8f9fa; }\n" +
                "    tr:hover { background-color: #e3f2fd; transition: background-color 0.2s; }\n" +
                "    .gpu-temp { font-weight: bold; }\n" +
                "    .temp-normal { color: #27ae60; }\n" +
                "    .temp-warm { color: #f39c12; }\n" +
                "    .temp-hot { color: #e74c3c; }\n" +
                "    .power-low { color: #27ae60; font-weight: bold; }\n" +
                "    .power-medium { color: #f39c12; font-weight: bold; }\n" +
                "    .power-high { color: #e74c3c; font-weight: bold; }\n" +
                "    .status-indicator { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 8px; background: #27ae60; animation: pulse 2s infinite; }\n" +
                "    @keyframes pulse { 0% { opacity: 1; } 50% { opacity: 0.5; } 100% { opacity: 1; } }\n" +
                "    @media (max-width: 768px) { main { flex-direction: column; } .sidebar { max-width: none; } }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <header>\n" +
                "    <span class=\"status-indicator\"></span>\n" +
                "    Real-time GPU & System Monitoring\n" +
                "  </header>\n" +
                "  <main>\n" +
                "    <div class=\"sidebar\">\n" +
                "      <div class=\"info-card\">\n" +
                "        <h3>ℹ️ System Information</h3>\n" +
                "        <div class=\"info-item\">\n" +
                "          <span class=\"info-label\">Operating System:</span>\n" +
                "          <span class=\"info-value\" id=\"osInfo\">Loading...</span>\n" +
                "        </div>\n" +
                "        <div class=\"info-item\">\n" +
                "          <span class=\"info-label\">Hostname:</span>\n" +
                "          <span class=\"info-value\" id=\"hostname\">Loading...</span>\n" +
                "        </div>\n" +
                "        <div class=\"info-item\">\n" +
                "          <span class=\"info-label\">Uptime:</span>\n" +
                "          <span class=\"info-value\" id=\"uptime\">Loading...</span>\n" +
                "        </div>\n" +
                "        <div class=\"info-item\">\n" +
                "          <span class=\"info-label\">CPU Cores:</span>\n" +
                "          <span class=\"info-value\" id=\"cpuCores\">--</span>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "      \n" +
                "      <div class=\"ollama-card\">\n" +
                "        <h3>🤖 Ollama Control</h3>\n" +
                "        <div class=\"ollama-status\" id=\"ollamaStatus\">Status: Checking...</div>\n" +
                "        <button class=\"ollama-btn btn-start\" onclick=\"controlOllama('start')\">Start Ollama</button>\n" +
                "        <button class=\"ollama-btn btn-stop\" onclick=\"controlOllama('stop')\">Stop Ollama</button>\n" +
                "        <button class=\"ollama-btn btn-restart\" onclick=\"controlOllama('restart')\">Restart Ollama</button>\n" +
                "      </div>\n" +
                "      \n" +
                "      <div class=\"system-card\">\n" +
                "        <h3>📊 Live Metrics</h3>\n" +
                "        <div class=\"metric\">\n" +
                "          <div class=\"metric-label\">CPU Usage</div>\n" +
                "          <div class=\"metric-value\" id=\"cpuUsage\">--%</div>\n" +
                "          <div class=\"progress-bar\"><div class=\"progress-fill\" id=\"cpuProgress\"></div></div>\n" +
                "        </div>\n" +
                "        <div class=\"metric\">\n" +
                "          <div class=\"metric-label\">RAM Usage</div>\n" +
                "          <div class=\"metric-value\" id=\"ramUsage\">-- / -- GB</div>\n" +
                "          <div class=\"progress-bar\"><div class=\"progress-fill\" id=\"ramProgress\"></div></div>\n" +
                "        </div>\n" +
                "        <div class=\"metric\">\n" +
                "          <div class=\"metric-label\">Disk Usage</div>\n" +
                "          <div class=\"metric-value\" id=\"diskUsage\">-- / -- GB</div>\n" +
                "          <div class=\"progress-bar\"><div class=\"progress-fill\" id=\"diskProgress\"></div></div>\n" +
                "        </div>\n" +
                "        <div class=\"metric\">\n" +
                "          <div class=\"metric-label\">CPU Temperature</div>\n" +
                "          <div class=\"metric-value\" id=\"cpuTemp\">--°C</div>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "      \n" +
                "      <div class=\"info-card\">\n" +
                "        <h3>🔗 About</h3>\n" +
                "        <div style=\"text-align: center; margin-bottom: 10px;\">\n" +
                "          <a href=\"https://github.com/Lejiww/Gpu-Monitor\" target=\"_blank\">GitHub Repository</a>\n" +
                "        </div>\n" +
                "        <div style=\"text-align: center; font-size: 11px; color: #7f8c8d; border-top: 1px solid #e0e0e0; padding-top: 8px;\">\n" +
                "          Made with &hearts; by Lejiww\n" +
                "        </div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"content\">\n" +
                "      <h2>🎮 GPU Statistics</h2>\n" +
                "      <div id=\"gpuInfo\">\n" +
                "        <p>Loading...</p>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "  </main>\n" +
                "  \n" +
                "  <script>\n" +
                "    function controlOllama(action) {\n" +
                "      fetch('/ollama/' + action)\n" +
                "        .then(response => response.json())\n" +
                "        .then(data => {\n" +
                "          if (data.success) {\n" +
                "            alert('Ollama ' + action + ' successful');\n" +
                "            checkOllamaStatus();\n" +
                "          } else {\n" +
                "            alert('Ollama ' + action + ' failed');\n" +
                "          }\n" +
                "        })\n" +
                "        .catch(error => alert('Error: ' + error));\n" +
                "    }\n" +
                "\n" +
                "    function checkOllamaStatus() {\n" +
                "      fetch('/ollama/status')\n" +
                "        .then(response => response.text())\n" +
                "        .then(status => {\n" +
                "          document.getElementById('ollamaStatus').textContent = 'Status: ' + status;\n" +
                "        })\n" +
                "        .catch(error => {\n" +
                "          document.getElementById('ollamaStatus').textContent = 'Status: Error';\n" +
                "        });\n" +
                "    }\n" +
                "\n" +
                "    function formatTemperature(temp) {\n" +
                "      const tempNum = parseInt(temp);\n" +
                "      let tempClass = 'temp-normal';\n" +
                "      if (tempNum > 80) tempClass = 'temp-hot';\n" +
                "      else if (tempNum > 65) tempClass = 'temp-warm';\n" +
                "      return '<span class=\"gpu-temp ' + tempClass + '\">' + temp + '°C</span>';\n" +
                "    }\n" +
                "\n" +
                "    function formatPowerConsumption(powerDraw, powerLimit) {\n" +
                "      const drawNum = parseInt(powerDraw);\n" +
                "      const limitNum = parseInt(powerLimit);\n" +
                "      const percentage = limitNum > 0 ? (drawNum / limitNum) * 100 : 0;\n" +
                "      \n" +
                "      let powerClass = 'power-low';\n" +
                "      if (percentage > 80) powerClass = 'power-high';\n" +
                "      else if (percentage > 50) powerClass = 'power-medium';\n" +
                "      \n" +
                "      return '<span class=\"' + powerClass + '\">' + powerDraw + 'W / ' + powerLimit + 'W</span>';\n" +
                "    }\n" +
                "\n" +
                "    function fetchGpuData() {\n" +
                "      fetch('/update')\n" +
                "        .then(function(response) {\n" +
                "          return response.text();\n" +
                "        })\n" +
                "        .then(function(data) {\n" +
                "          const rows = data.trim().split('\\n');\n" +
                "          let tableHtml = '';\n" +
                "          \n" +
                "          for (let index = 0; index < rows.length; index++) {\n" +
                "            const row = rows[index];\n" +
                "            const parts = row.split(',');\n" +
                "            \n" +
                "            if (parts.length < 7) continue;\n" +
                "            \n" +
                "            const name = parts[0] || 'Unknown GPU';\n" +
                "            const temp = parts[1] || '0';\n" +
                "            const util = parts[2] || '0';\n" +
                "            const memUsed = parts[3] || '0';\n" +
                "            const memTotal = parts[4] || '0';\n" +
                "            const powerDraw = parts[5] || '0';\n" +
                "            const powerLimit = parts[6] || '0';\n" +
                "            \n" +
                "            tableHtml += '<tr>';\n" +
                "            tableHtml += '<td><strong>GPU ' + index + '</strong><br>' + name + '</td>';\n" +
                "            tableHtml += '<td>' + formatTemperature(temp) + '</td>';\n" +
                "            tableHtml += '<td><strong>' + util + '%</strong></td>';\n" +
                "            tableHtml += '<td>' + memUsed + ' MB / ' + memTotal + ' MB</td>';\n" +
                "            tableHtml += '<td>' + formatPowerConsumption(powerDraw, powerLimit) + '</td>';\n" +
                "            tableHtml += '</tr>';\n" +
                "          }\n" +
                "\n" +
                "          document.getElementById('gpuInfo').innerHTML = '<table>' +\n" +
                "            '<tr>' +\n" +
                "            '<th>GPU</th>' +\n" +
                "            '<th>Temperature</th>' +\n" +
                "            '<th>Utilization</th>' +\n" +
                "            '<th>Memory</th>' +\n" +
                "            '<th>Power Consumption</th>' +\n" +
                "            '</tr>' +\n" +
                "            tableHtml +\n" +
                "            '</table>';\n" +
                "        })\n" +
                "        .catch(function(error) {\n" +
                "          document.getElementById('gpuInfo').innerHTML = '<p style=\"color:red;\">Error: ' + error.message + '</p>';\n" +
                "        });\n" +
                "    }\n" +
                "\n" +
                "    function fetchSystemData() {\n" +
                "      fetch('/system')\n" +
                "        .then(function(response) {\n" +
                "          return response.json();\n" +
                "        })\n" +
                "        .then(function(data) {\n" +
                "          if (data.cpuUsage !== undefined) {\n" +
                "            document.getElementById('cpuUsage').textContent = data.cpuUsage + '%';\n" +
                "            document.getElementById('cpuProgress').style.width = data.cpuUsage + '%';\n" +
                "          }\n" +
                "          \n" +
                "          if (data.ramUsed && data.ramTotal) {\n" +
                "            document.getElementById('ramUsage').textContent = data.ramUsed + ' / ' + data.ramTotal + ' GB';\n" +
                "            document.getElementById('ramProgress').style.width = data.ramPercentage + '%';\n" +
                "          }\n" +
                "          \n" +
                "          if (data.diskUsed && data.diskTotal) {\n" +
                "            document.getElementById('diskUsage').textContent = data.diskUsed + ' / ' + data.diskTotal + ' GB';\n" +
                "            document.getElementById('diskProgress').style.width = data.diskPercentage + '%';\n" +
                "          }\n" +
                "          \n" +
                "          if (data.cpuTemp !== undefined) {\n" +
                "            document.getElementById('cpuTemp').textContent = data.cpuTemp + '°C';\n" +
                "          }\n" +
                "          \n" +
                "          if (data.osInfo) {\n" +
                "            document.getElementById('osInfo').textContent = data.osInfo;\n" +
                "          }\n" +
                "          \n" +
                "          if (data.hostname) {\n" +
                "            document.getElementById('hostname').textContent = data.hostname;\n" +
                "          }\n" +
                "          \n" +
                "          if (data.uptime) {\n" +
                "            document.getElementById('uptime').textContent = data.uptime;\n" +
                "          }\n" +
                "          \n" +
                "          if (data.cpuCores !== undefined) {\n" +
                "            document.getElementById('cpuCores').textContent = data.cpuCores;\n" +
                "          }\n" +
                "        })\n" +
                "        .catch(function(error) {\n" +
                "          console.error('System error:', error);\n" +
                "        });\n" +
                "    }\n" +
                "\n" +
                "    console.log('Starting monitoring...');\n" +
                "    fetchGpuData();\n" +
                "    fetchSystemData();\n" +
                "    checkOllamaStatus();\n" +
                "    \n" +
                "    setInterval(fetchGpuData, 2000);\n" +
                "    setInterval(fetchSystemData, 2000);\n" +
                "    setInterval(checkOllamaStatus, 5000);\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";
    }

    private void handleDebugPageRequest(BufferedWriter out) throws IOException {
        String debugInfo = getDebugInfo();
        String debugHtml = "<!DOCTYPE html><html><head><title>Debug</title></head><body><pre>" + debugInfo + "</pre></body></html>";
        
        out.write("HTTP/1.1 200 OK\r\n");
        out.write("Content-Type: text/html; charset=UTF-8\r\n");
        out.write("Content-Length: " + debugHtml.length() + "\r\n");
        out.write("\r\n");
        out.write(debugHtml);
        out.flush();
    }

    private String getDebugInfo() {
        return "System Debug - Basic Info\n" +
               "OS: " + System.getProperty("os.name") + "\n" +
               "Java: " + System.getProperty("java.version");
    }
}