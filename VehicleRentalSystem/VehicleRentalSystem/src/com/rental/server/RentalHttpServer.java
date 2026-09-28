package com.rental.server;

import com.rental.model.*;
import com.rental.service.RentalAdmin;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Built-in Lightweight HTTP & REST API Server for the Vehicle Rental System.
 * Uses standard Java SE HttpServer with ZERO external dependencies.
 * Bridges the Java RentalAdmin OOP core with the elegant HTML/CSS/JS web frontend.
 */
public class RentalHttpServer {

    private static final int PORT = 8080;
    private final RentalAdmin admin;
    private final Path webRootDir;

    public RentalHttpServer(RentalAdmin admin, Path webRootDir) {
        this.admin = admin;
        this.webRootDir = webRootDir.toAbsolutePath().normalize();
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // API Endpoints
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/vehicles", new VehiclesHandler());
        server.createContext("/api/customers", new CustomersHandler());
        server.createContext("/api/rentals", new RentalsHandler());
        server.createContext("/api/book", new BookHandler());
        server.createContext("/api/return", new ReturnHandler());
        server.createContext("/api/reset", new ResetHandler());

        // Static Asset Handler for Web UI
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null); // default multi-threading
        server.start();

        System.out.println("==========================================================");
        System.out.println("  VEHICLE RENTAL SYSTEM - WEB BACKEND ONLINE");
        System.out.println("  URL: http://localhost:" + PORT + "/");
        System.out.println("  Serving frontend from: " + webRootDir.toAbsolutePath());
        System.out.println("==========================================================");
    }

    public static void main(String[] args) {
        RentalAdmin admin = new RentalAdmin();
        admin.seedDefaultData();

        // Locate web directory relative to working dir
        Path webDir = Paths.get("web");
        if (!Files.exists(webDir)) {
            webDir = Paths.get("../web");
        }
        if (!Files.exists(webDir)) {
            webDir = Paths.get("../../web");
        }

        try {
            RentalHttpServer server = new RentalHttpServer(admin, webDir);
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start HTTP server: " + e.getMessage());
        }
    }

    // =========================================================
    // HANDLERS
    // =========================================================

    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            int totalVehicles = admin.getAllVehicles().size();
            int available = admin.getAvailableVehicles().size();
            int rented = admin.getRentedVehicles().size();
            int activeRentals = admin.getActiveRentals().size();
            int totalCustomers = admin.getAllCustomers().size();
            double totalRevenue = admin.getAllRentals().stream().mapToDouble(Rental::getRentalCost).sum();

            String json = String.format(java.util.Locale.US,
                    "{\"totalVehicles\":%d,\"availableVehicles\":%d,\"rentedVehicles\":%d," +
                    "\"activeRentals\":%d,\"totalCustomers\":%d,\"totalRevenue\":%.2f,\"status\":\"ONLINE\"}",
                    totalVehicles, available, rented, activeRentals, totalCustomers, totalRevenue);

            sendJsonResponse(exchange, 200, json);
        }
    }

    private class VehiclesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();

            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equals(method)) {
                StringBuilder sb = new StringBuilder("[");
                List<Vehicle> list = admin.getAllVehicles();
                for (int i = 0; i < list.size(); i++) {
                    Vehicle v = list.get(i);
                    sb.append(vehicleToJson(v));
                    if (i < list.size() - 1) sb.append(",");
                }
                sb.append("]");
                sendJsonResponse(exchange, 200, sb.toString());
                return;
            }

            if ("POST".equals(method)) {
                String body = readBody(exchange);
                Map<String, String> params = parseJsonOrForm(body);

                String id = params.get("id");
                String typeStr = params.getOrDefault("type", "CAR");
                String brand = params.get("brand");
                String model = params.get("model");
                double rate = parseDoubleSafe(params.get("rentalRate"), 50.0);

                if (id == null || brand == null || model == null) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Missing required fields: id, brand, model\"}");
                    return;
                }

                VehicleType type;
                try {
                    type = VehicleType.valueOf(typeStr.toUpperCase());
                } catch (Exception e) {
                    type = VehicleType.CAR;
                }

                Vehicle vehicle;
                if (type == VehicleType.CAR) {
                    int seats = (int) parseDoubleSafe(params.get("seatingCapacity"), 5);
                    String fuel = params.getOrDefault("fuelType", "Petrol");
                    vehicle = new Car(id, brand, model, rate, seats, fuel);
                } else if (type == VehicleType.BIKE) {
                    int cc = (int) parseDoubleSafe(params.get("engineCapacityCc"), 350);
                    boolean helmet = Boolean.parseBoolean(params.getOrDefault("helmetIncluded", "true"));
                    vehicle = new Bike(id, brand, model, rate, cc, helmet);
                } else {
                    double cargo = parseDoubleSafe(params.get("cargoCapacityKg"), 1000.0);
                    boolean sliding = Boolean.parseBoolean(params.getOrDefault("hasSlidingDoor", "true"));
                    vehicle = new Van(id, brand, model, rate, cargo, sliding);
                }

                if (admin.findVehicleById(id) != null) {
                    sendJsonResponse(exchange, 409, "{\"error\":\"Vehicle ID already exists\"}");
                    return;
                }

                admin.addVehicle(vehicle);
                sendJsonResponse(exchange, 201, vehicleToJson(vehicle));
                return;
            }

            if ("DELETE".equals(method)) {
                String query = exchange.getRequestURI().getQuery();
                String id = queryParam(query, "id");
                if (id == null) {
                    String body = readBody(exchange);
                    Map<String, String> p = parseJsonOrForm(body);
                    id = p.get("id");
                }

                if (id == null) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Missing vehicle id\"}");
                    return;
                }

                boolean removed = admin.removeVehicle(id);
                if (removed) {
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Vehicle removed\"}");
                } else {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Cannot remove vehicle (not found or currently rented)\"}");
                }
                return;
            }

            if ("PUT".equals(method)) {
                String body = readBody(exchange);
                Map<String, String> p = parseJsonOrForm(body);
                String id = p.get("id");
                String brand = p.get("brand");
                String model = p.get("model");
                double rate = parseDoubleSafe(p.get("rentalRate"), -1);

                boolean updated = admin.updateVehicle(id, brand, model, rate);
                if (updated) {
                    Vehicle v = admin.findVehicleById(id);
                    sendJsonResponse(exchange, 200, vehicleToJson(v));
                } else {
                    sendJsonResponse(exchange, 404, "{\"error\":\"Vehicle not found\"}");
                }
                return;
            }

            sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
        }
    }

    private class CustomersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();

            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equals(method)) {
                StringBuilder sb = new StringBuilder("[");
                List<Customer> list = admin.getAllCustomers();
                for (int i = 0; i < list.size(); i++) {
                    Customer c = list.get(i);
                    sb.append(customerToJson(c));
                    if (i < list.size() - 1) sb.append(",");
                }
                sb.append("]");
                sendJsonResponse(exchange, 200, sb.toString());
                return;
            }

            if ("POST".equals(method)) {
                String body = readBody(exchange);
                Map<String, String> p = parseJsonOrForm(body);
                String id = p.get("id");
                String name = p.get("name");
                String contact = p.get("contactDetails");
                String licence = p.get("licenceNumber");

                if (id == null || name == null) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Missing id or name\"}");
                    return;
                }

                if (admin.findCustomerById(id) != null) {
                    sendJsonResponse(exchange, 409, "{\"error\":\"Customer ID already exists\"}");
                    return;
                }

                Customer customer = new Customer(id, name, contact != null ? contact : "", licence != null ? licence : "DL-" + id + "001");
                admin.addCustomer(customer);
                sendJsonResponse(exchange, 201, customerToJson(customer));
                return;
            }

            sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
        }
    }

    private class RentalsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            StringBuilder sb = new StringBuilder("[");
            List<Rental> list = admin.getAllRentals();
            for (int i = 0; i < list.size(); i++) {
                sb.append(rentalToJson(list.get(i)));
                if (i < list.size() - 1) sb.append(",");
            }
            sb.append("]");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    private class BookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readBody(exchange);
            Map<String, String> p = parseJsonOrForm(body);

            String customerId = p.get("customerId");
            String vehicleId = p.get("vehicleId");
            String startDateStr = p.get("startDate");
            String endDateStr = p.get("endDate");
            double deposit = parseDoubleSafe(p.get("securityDeposit"), 200.0);

            Customer customer = admin.findCustomerById(customerId);
            if (customer == null) {
                sendJsonResponse(exchange, 404, "{\"error\":\"Customer not found\"}");
                return;
            }

            Vehicle vehicle = admin.findVehicleById(vehicleId);
            if (vehicle == null) {
                sendJsonResponse(exchange, 404, "{\"error\":\"Vehicle not found\"}");
                return;
            }

            if (vehicle.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
                sendJsonResponse(exchange, 400, "{\"error\":\"Vehicle is not available for booking\"}");
                return;
            }

            LocalDate start, end;
            try {
                start = LocalDate.parse(startDateStr);
                end = LocalDate.parse(endDateStr);
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"Invalid date format (must be YYYY-MM-DD)\"}");
                return;
            }

            if (!end.isAfter(start)) {
                sendJsonResponse(exchange, 400, "{\"error\":\"End date must be strictly after start date\"}");
                return;
            }

            Rental rental = admin.bookVehicle(customer, vehicle, start, end, deposit);
            if (rental != null) {
                sendJsonResponse(exchange, 201, rentalToJson(rental));
            } else {
                sendJsonResponse(exchange, 400, "{\"error\":\"Booking failed\"}");
            }
        }
    }

    private class ReturnHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readBody(exchange);
            Map<String, String> p = parseJsonOrForm(body);

            String rentalId = p.get("rentalId");
            String returnDateStr = p.get("actualReturnDate");

            Rental rental = admin.findRentalById(rentalId);
            if (rental == null) {
                sendJsonResponse(exchange, 404, "{\"error\":\"Rental record not found\"}");
                return;
            }

            if (!rental.isActive()) {
                sendJsonResponse(exchange, 400, "{\"error\":\"Rental is already closed/returned\"}");
                return;
            }

            LocalDate returnDate;
            try {
                returnDate = returnDateStr != null && !returnDateStr.isEmpty() ? LocalDate.parse(returnDateStr) : LocalDate.now();
            } catch (Exception e) {
                returnDate = LocalDate.now();
            }

            double refund = admin.returnVehicle(rentalId, returnDate);
            String json = String.format(java.util.Locale.US,
                    "{\"success\":true,\"rentalId\":\"%s\",\"refund\":%.2f,\"latePenalty\":%.2f,\"actualReturnDate\":\"%s\"}",
                    rental.getRentalId(), refund, rental.getLateReturnPenalty(), returnDate);

            sendJsonResponse(exchange, 200, json);
        }
    }

    private class ResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            admin.seedDefaultData();
            sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Default data reset successfully\"}");
        }
    }

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }

            Path file = webRootDir.resolve(path.substring(1)).normalize();

            // Safety check against path traversal
            if (!file.startsWith(webRootDir.toAbsolutePath()) && !file.toAbsolutePath().startsWith(webRootDir.toAbsolutePath())) {
                send404(exchange);
                return;
            }

            if (!Files.exists(file) || Files.isDirectory(file)) {
                // Try index.html in directory
                Path alt = file.resolve("index.html");
                if (Files.exists(alt)) {
                    file = alt;
                } else {
                    send404(exchange);
                    return;
                }
            }

            String contentType = getMimeType(file.getFileName().toString());
            byte[] bytes = Files.readAllBytes(file);

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // =========================================================
    // JSON & STRING HELPERS
    // =========================================================

    private static String vehicleToJson(Vehicle v) {
        return String.format(java.util.Locale.US,
                "{\"id\":\"%s\",\"type\":\"%s\",\"brand\":\"%s\",\"model\":\"%s\",\"rentalRate\":%.2f,\"status\":\"%s\",\"details\":\"%s\"}",
                escape(v.getVehicleId()), escape(v.getType().name()), escape(v.getBrand()),
                escape(v.getModel()), v.getRentalRate(), v.getAvailabilityStatus().name(),
                escape(v.getSpecificDetails()));
    }

    private static String customerToJson(Customer c) {
        return String.format(java.util.Locale.US,
                "{\"id\":\"%s\",\"name\":\"%s\",\"contactDetails\":\"%s\",\"licenceNumber\":\"%s\",\"totalRentals\":%d}",
                escape(c.getCustomerId()), escape(c.getName()), escape(c.getContactDetails()),
                escape(c.getLicenceNumber()),
                c.getRentalHistory().size());
    }

    private static String rentalToJson(Rental r) {
        String actual = r.getActualReturnDate() != null ? r.getActualReturnDate().toString() : "";
        return String.format(java.util.Locale.US,
                "{\"rentalId\":\"%s\",\"customerId\":\"%s\",\"customerName\":\"%s\",\"vehicleId\":\"%s\"," +
                "\"vehicleDetails\":\"%s %s\",\"startDate\":\"%s\",\"endDate\":\"%s\",\"rentalCost\":%.2f," +
                "\"securityDeposit\":%.2f,\"latePenalty\":%.2f,\"refund\":%.2f,\"active\":%b,\"actualReturnDate\":\"%s\"}",
                escape(r.getRentalId()), escape(r.getCustomer().getCustomerId()), escape(r.getCustomer().getName()),
                escape(r.getVehicle().getVehicleId()), escape(r.getVehicle().getBrand()), escape(r.getVehicle().getModel()),
                r.getStartDate(), r.getEndDate(), r.getRentalCost(), r.getSecurityDeposit(),
                r.getLateReturnPenalty(), r.getRefundAmount(), r.isActive(), actual);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        addCorsHeaders(exchange);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void send404(HttpExchange exchange) throws IOException {
        String response = "<h1>404 Not Found</h1>";
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(404, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseJsonOrForm(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.trim().isEmpty()) return map;

        String trimmed = body.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            // Simple robust regex key-value extractor for JSON without external libs
            String content = trimmed.substring(1, trimmed.length() - 1);
            String[] tokens = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (String token : tokens) {
                String[] kv = token.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (kv.length == 2) {
                    String key = kv[0].trim().replace("\"", "");
                    String val = kv[1].trim().replace("\"", "");
                    map.put(key, val);
                }
            }
        } else {
            // URL Form encoded
            String[] pairs = trimmed.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                try {
                    if (idx > 0) {
                        String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                        String v = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                        map.put(k, v);
                    }
                } catch (Exception ignored) {}
            }
        }
        return map;
    }

    private static String queryParam(String query, String name) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && kv[0].equalsIgnoreCase(name)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static double parseDoubleSafe(String val, double defaultVal) {
        if (val == null) return defaultVal;
        try {
            return Double.parseDouble(val.trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private static String getMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}
