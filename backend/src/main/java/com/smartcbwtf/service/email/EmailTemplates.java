package com.smartcbwtf.service.email;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Professional email templates for SmartCBWTF platform.
 * All emails follow consistent branding with green gradient header.
 */
@Component
public class EmailTemplates {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    /**
     * Base HTML template wrapper with SmartCBWTF branding.
     */
    public String wrapInTemplate(String title, String content) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                    <style>
                        body { margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7fa; }
                        .container { max-width: 600px; margin: 0 auto; background: #ffffff; }
                        .header { background: linear-gradient(135deg, #1a8754 0%%, #20c997 100%%); padding: 30px 40px; text-align: center; }
                        .header h1 { color: #ffffff; margin: 0; font-size: 28px; font-weight: 600; letter-spacing: 1px; }
                        .header p { color: rgba(255,255,255,0.9); margin: 8px 0 0 0; font-size: 14px; }
                        .content { padding: 40px; color: #333333; line-height: 1.7; }
                        .content h2 { color: #1a8754; margin-top: 0; font-size: 22px; }
                        .info-box { background: #f8f9fa; border-left: 4px solid #1a8754; padding: 20px; margin: 20px 0; border-radius: 0 8px 8px 0; }
                        .info-box p { margin: 8px 0; }
                        .info-box strong { color: #1a8754; }
                        .credentials-box { background: linear-gradient(135deg, #e8f5e9 0%%, #c8e6c9 100%%); padding: 25px; margin: 25px 0; border-radius: 12px; border: 1px solid #a5d6a7; }
                        .credentials-box h3 { margin-top: 0; color: #1a8754; }
                        .credential { background: #ffffff; padding: 12px 18px; margin: 10px 0; border-radius: 8px; font-family: 'Courier New', monospace; font-size: 15px; border: 1px solid #c8e6c9; }
                        .warning-box { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px 20px; margin: 20px 0; border-radius: 0 8px 8px 0; }
                        .success-box { background: #d4edda; border-left: 4px solid #28a745; padding: 15px 20px; margin: 20px 0; border-radius: 0 8px 8px 0; }
                        .error-box { background: #f8d7da; border-left: 4px solid #dc3545; padding: 15px 20px; margin: 20px 0; border-radius: 0 8px 8px 0; }
                        .btn { display: inline-block; background: linear-gradient(135deg, #1a8754 0%%, #20c997 100%%); color: #ffffff !important; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: 600; margin: 20px 0; }
                        .btn:hover { opacity: 0.9; }
                        .table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                        .table th { background: #1a8754; color: #ffffff; padding: 12px; text-align: left; }
                        .table td { padding: 12px; border-bottom: 1px solid #e9ecef; }
                        .table tr:nth-child(even) { background: #f8f9fa; }
                        .amount { font-size: 24px; font-weight: bold; color: #1a8754; }
                        .footer { background: #2d3436; padding: 30px 40px; text-align: center; }
                        .footer p { color: #b2bec3; margin: 5px 0; font-size: 13px; }
                        .footer a { color: #20c997; text-decoration: none; }
                        .divider { height: 1px; background: linear-gradient(90deg, transparent, #1a8754, transparent); margin: 30px 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>SmartCBWTF</h1>
                            <p>Biomedical Waste Management Portal</p>
                        </div>
                        <div class="content">
                            %s
                        </div>
                        <div class="footer">
                            <p><strong>SmartCBWTF</strong> - Comprehensive Biomedical Waste Tracking</p>
                            <p>This is an automated email. Please do not reply directly.</p>
                            <p>For support, contact: <a href="mailto:support@smartcbwtf.com">support@smartcbwtf.com</a></p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(title, content);
    }

    // ==================== REGISTRATION & ONBOARDING ====================

    public String cbwtfWelcome(String facilityName, String adminName, String username, String password,
            String portalUrl) {
        String content = """
                <h2>Welcome to SmartCBWTF!</h2>
                <p>Dear <strong>%s</strong>,</p>
                <p>Congratulations! Your CBWTF facility <strong>%s</strong> has been successfully onboarded to the SmartCBWTF platform.</p>

                <div class="credentials-box">
                    <h3>🔐 Your Admin Login Credentials</h3>
                    <p><strong>Username:</strong></p>
                    <div class="credential">%s</div>
                    <p><strong>Temporary Password:</strong></p>
                    <div class="credential">%s</div>
                </div>

                <div class="warning-box">
                    <strong>⚠️ Important:</strong> Please change your password immediately after your first login for security.
                </div>

                <p style="text-align: center;">
                    <a href="%s" class="btn">Access Portal →</a>
                </p>

                <div class="divider"></div>

                <h3>Getting Started</h3>
                <ul>
                    <li>Complete your facility profile</li>
                    <li>Add staff members and assign roles</li>
                    <li>Register HCFs and manage agreements</li>
                    <li>Configure billing and consumables</li>
                </ul>

                <p>Our team is here to support you. Welcome aboard!</p>
                """
                .formatted(adminName, facilityName, username, password, portalUrl);
        return wrapInTemplate("Welcome to SmartCBWTF", content);
    }

    public String hcfRegistrationReceived(String hcfName, String cbwtfName, String agreementNumber) {
        String content = """
                <h2>Registration Received</h2>
                <p>Dear <strong>%s</strong>,</p>
                <p>Thank you for registering with <strong>%s</strong> for biomedical waste management services.</p>

                <div class="info-box">
                    <p><strong>Application Reference:</strong> %s</p>
                    <p><strong>Status:</strong> Under Review</p>
                    <p><strong>Submitted:</strong> %s</p>
                </div>

                <h3>What's Next?</h3>
                <ol>
                    <li>Our team will review your registration details</li>
                    <li>You may be contacted for additional documentation</li>
                    <li>Once approved, you'll receive your portal login credentials</li>
                </ol>

                <p>Expected processing time: <strong>2-3 business days</strong></p>

                <p>If you have any questions, please contact us.</p>
                """.formatted(hcfName, cbwtfName, agreementNumber, LocalDateTime.now().format(DATETIME_FORMAT));
        return wrapInTemplate("Registration Received - " + agreementNumber, content);
    }

    public String hcfApproved(String hcfName, String agreementNumber, String startDate, String endDate) {
        String content = """
                <h2>🎉 Registration Approved!</h2>
                <p>Dear <strong>%s</strong>,</p>
                <p>Great news! Your registration has been <strong>approved</strong>.</p>

                <div class="success-box">
                    <strong>✅ Your agreement is now active</strong>
                </div>

                <div class="info-box">
                    <p><strong>Agreement Number:</strong> %s</p>
                    <p><strong>Valid From:</strong> %s</p>
                    <p><strong>Valid Until:</strong> %s</p>
                </div>

                <p>Your portal login credentials will be sent in a separate email.</p>

                <h3>Services Now Available</h3>
                <ul>
                    <li>Biomedical waste collection and disposal</li>
                    <li>Online billing and payment tracking</li>
                    <li>Digital waste tracking with QR codes</li>
                    <li>Order consumables (bags, labels)</li>
                </ul>

                <p>Thank you for choosing us for your biomedical waste management needs.</p>
                """.formatted(hcfName, agreementNumber, startDate, endDate);
        return wrapInTemplate("Registration Approved - " + agreementNumber, content);
    }

    public String hcfRejected(String hcfName, String reason) {
        String content = """
                <h2>Registration Status Update</h2>
                <p>Dear <strong>%s</strong>,</p>
                <p>We regret to inform you that your registration application could not be approved at this time.</p>

                <div class="error-box">
                    <strong>Reason:</strong> %s
                </div>

                <h3>Next Steps</h3>
                <p>You may resubmit your application after addressing the issues mentioned above. Please ensure all required documents are complete and accurate.</p>

                <p>If you believe this decision was made in error or need clarification, please contact our support team.</p>
                """
                .formatted(hcfName, reason);
        return wrapInTemplate("Registration Update", content);
    }

    public String hcfCredentials(String hcfName, String username, String password, String portalUrl) {
        String content = """
                <h2>Your Portal Access Credentials</h2>
                <p>Dear <strong>%s</strong>,</p>
                <p>Your HCF portal account has been created. Use the credentials below to access your dashboard.</p>

                <div class="credentials-box">
                    <h3>🔐 Login Credentials</h3>
                    <p><strong>Username:</strong></p>
                    <div class="credential">%s</div>
                    <p><strong>Password:</strong></p>
                    <div class="credential">%s</div>
                </div>

                <div class="warning-box">
                    <strong>⚠️ Security Notice:</strong> Please change your password after first login. Do not share these credentials.
                </div>

                <p style="text-align: center;">
                    <a href="%s" class="btn">Login to Portal →</a>
                </p>

                <h3>Portal Features</h3>
                <ul>
                    <li>View waste collection history</li>
                    <li>Track invoices and payments</li>
                    <li>Order consumables</li>
                    <li>Download reports</li>
                </ul>
                """
                .formatted(hcfName, username, password, portalUrl);
        return wrapInTemplate("Portal Access - " + hcfName, content);
    }

    public String staffCredentials(String staffName, String role, String username, String password,
            String facilityName) {
        String content = """
                <h2>Staff Account Created</h2>
                <p>Dear <strong>%s</strong>,</p>
                <p>A staff account has been created for you at <strong>%s</strong>.</p>

                <div class="info-box">
                    <p><strong>Role:</strong> %s</p>
                </div>

                <div class="credentials-box">
                    <h3>🔐 Your Login Credentials</h3>
                    <p><strong>Username:</strong></p>
                    <div class="credential">%s</div>
                    <p><strong>Password:</strong></p>
                    <div class="credential">%s</div>
                </div>

                <div class="warning-box">
                    <strong>⚠️ Important:</strong> Change your password after first login.
                </div>

                <p>Contact your administrator if you have any questions.</p>
                """.formatted(staffName, facilityName, role, username, password);
        return wrapInTemplate("Staff Account Created", content);
    }

    // ==================== CREDENTIALS & SECURITY ====================

    public String passwordReset(String userName, String newPassword) {
        String content = """
                <h2>Password Reset</h2>
                <p>Dear <strong>%s</strong>,</p>
                <p>Your password has been reset by an administrator.</p>

                <div class="credentials-box">
                    <h3>🔐 New Credentials</h3>
                    <p><strong>New Password:</strong></p>
                    <div class="credential">%s</div>
                </div>

                <div class="warning-box">
                    <strong>⚠️ Security:</strong> If you did not request this change, contact support immediately.
                </div>

                <p>Please change this password after logging in.</p>
                """.formatted(userName, newPassword);
        return wrapInTemplate("Password Reset", content);
    }

    public String accountLocked(String userName, String reason) {
        String content = """
                <h2>⚠️ Account Security Alert</h2>
                <p>Dear <strong>%s</strong>,</p>
                <p>Your account has been temporarily locked for security reasons.</p>

                <div class="error-box">
                    <strong>Reason:</strong> %s
                </div>

                <p>To unlock your account, please contact your administrator or our support team.</p>

                <p>If you did not attempt to log in, your account credentials may be compromised. Please reset your password immediately after unlocking.</p>
                """
                .formatted(userName, reason);
        return wrapInTemplate("Account Locked", content);
    }

    // ==================== ORDERS ====================

    public String orderPlacedHcf(String hcfName, String orderNumber, String cbwtfName, String itemsHtml, String total) {
        String content = """
                <h2>Order Confirmation</h2>
                <p>Dear <strong>%s</strong>,</p>
                <p>Your consumable order has been placed successfully.</p>

                <div class="success-box">
                    <strong>✅ Order Placed Successfully</strong>
                </div>

                <div class="info-box">
                    <p><strong>Order Number:</strong> %s</p>
                    <p><strong>CBWTF:</strong> %s</p>
                    <p><strong>Order Date:</strong> %s</p>
                </div>

                <h3>Order Items</h3>
                %s

                <p class="amount">Total: ₹%s (incl. GST)</p>

                <div class="divider"></div>

                <p>You will receive updates as your order is processed and dispatched.</p>
                """.formatted(hcfName, orderNumber, cbwtfName, LocalDateTime.now().format(DATETIME_FORMAT), itemsHtml,
                total);
        return wrapInTemplate("Order Confirmation - " + orderNumber, content);
    }

    public String orderPlacedCbwtf(String orderNumber, String hcfName, String hcfCode, String itemsHtml, String total,
            String notes) {
        String content = """
                <h2>New Consumable Order</h2>
                <p>A new order has been received and requires your attention.</p>

                <div class="info-box">
                    <p><strong>Order Number:</strong> %s</p>
                    <p><strong>From HCF:</strong> %s (%s)</p>
                    <p><strong>Order Date:</strong> %s</p>
                </div>

                <h3>Order Items</h3>
                %s

                <p class="amount">Total: ₹%s (incl. GST)</p>

                %s

                <p style="text-align: center;">
                    <a href="#" class="btn">Review Order →</a>
                </p>
                """.formatted(orderNumber, hcfName, hcfCode, LocalDateTime.now().format(DATETIME_FORMAT), itemsHtml,
                total,
                notes != null ? "<div class='warning-box'><strong>Customer Notes:</strong> " + notes + "</div>" : "");
        return wrapInTemplate("New Order - " + orderNumber, content);
    }

    public String orderStatusUpdate(String hcfName, String orderNumber, String status, String statusMessage) {
        String statusColor = switch (status) {
            case "CONFIRMED" -> "#17a2b8";
            case "DISPATCHED" -> "#fd7e14";
            case "DELIVERED" -> "#28a745";
            default -> "#6c757d";
        };

        String content = """
                <h2>Order Status Update</h2>
                <p>Dear <strong>%s</strong>,</p>
                <p>Your order status has been updated.</p>

                <div class="info-box">
                    <p><strong>Order Number:</strong> %s</p>
                    <p><strong>New Status:</strong> <span style="color: %s; font-weight: bold;">%s</span></p>
                    <p><strong>Updated:</strong> %s</p>
                </div>

                <p>%s</p>
                """.formatted(hcfName, orderNumber, statusColor, status, LocalDateTime.now().format(DATETIME_FORMAT),
                statusMessage);
        return wrapInTemplate("Order Update - " + orderNumber, content);
    }

    public String orderCancelled(String recipientName, String orderNumber, String reason, boolean isHcf) {
        String content = """
                <h2>Order Cancelled</h2>
                <p>Dear <strong>%s</strong>,</p>
                <p>%s</p>

                <div class="error-box">
                    <p><strong>Order Number:</strong> %s</p>
                    <p><strong>Reason:</strong> %s</p>
                    <p><strong>Cancelled:</strong> %s</p>
                </div>

                <p>If you have any questions, please contact %s.</p>
                """.formatted(recipientName,
                isHcf ? "Your order has been cancelled." : "An order has been cancelled by the HCF.",
                orderNumber, reason, LocalDateTime.now().format(DATETIME_FORMAT),
                isHcf ? "your CBWTF" : "the HCF");
        return wrapInTemplate("Order Cancelled - " + orderNumber, content);
    }

    // ==================== BILLING & PAYMENTS ====================

    public String invoiceGenerated(String hcfName, String invoiceNumber, String period, String amount, String dueDate) {
        String content = """
                <h2>Invoice Generated</h2>
                <p>Dear <strong>%s</strong>,</p>
                <p>A new invoice has been generated for your biomedical waste management services.</p>

                <div class="info-box">
                    <p><strong>Invoice Number:</strong> %s</p>
                    <p><strong>Billing Period:</strong> %s</p>
                    <p><strong>Due Date:</strong> %s</p>
                </div>

                <p class="amount">Amount Due: ₹%s</p>

                <p style="text-align: center;">
                    <a href="#" class="btn">View Invoice →</a>
                </p>

                <h3>Payment Options</h3>
                <ul>
                    <li>Online payment through the portal</li>
                    <li>Bank transfer (details in invoice)</li>
                    <li>Payment at CBWTF office</li>
                </ul>

                <p>Please ensure timely payment to avoid any service interruptions.</p>
                """.formatted(hcfName, invoiceNumber, period, dueDate, amount);
        return wrapInTemplate("Invoice - " + invoiceNumber, content);
    }

    public String paymentReminder(String hcfName, String invoiceNumber, String amount, String dueDate,
            int daysOverdue) {
        String urgency = daysOverdue > 0 ? "OVERDUE" : "DUE SOON";
        String boxClass = daysOverdue > 0 ? "error-box" : "warning-box";

        String content = """
                <h2>Payment %s</h2>
                <p>Dear <strong>%s</strong>,</p>

                <div class="%s">
                    <p><strong>Invoice:</strong> %s</p>
                    <p><strong>Amount:</strong> ₹%s</p>
                    <p><strong>Due Date:</strong> %s</p>
                    %s
                </div>

                <p>Please make the payment at your earliest convenience to avoid any service disruptions.</p>

                <p style="text-align: center;">
                    <a href="#" class="btn">Pay Now →</a>
                </p>
                """.formatted(urgency, hcfName, boxClass, invoiceNumber, amount, dueDate,
                daysOverdue > 0 ? "<p><strong>Days Overdue:</strong> " + daysOverdue + "</p>" : "");
        return wrapInTemplate("Payment " + urgency, content);
    }

    public String paymentReceived(String hcfName, String invoiceNumber, String amount, String paymentMethod,
            String transactionId) {
        String content = """
                <h2>Payment Received</h2>
                <p>Dear <strong>%s</strong>,</p>
                <p>Thank you! We have received your payment.</p>

                <div class="success-box">
                    <strong>✅ Payment Successful</strong>
                </div>

                <div class="info-box">
                    <p><strong>Invoice:</strong> %s</p>
                    <p><strong>Amount Paid:</strong> ₹%s</p>
                    <p><strong>Payment Method:</strong> %s</p>
                    <p><strong>Transaction ID:</strong> %s</p>
                    <p><strong>Date:</strong> %s</p>
                </div>

                <p>A receipt has been generated and is available in your portal.</p>
                """.formatted(hcfName, invoiceNumber, amount, paymentMethod, transactionId,
                LocalDateTime.now().format(DATETIME_FORMAT));
        return wrapInTemplate("Payment Received - " + invoiceNumber, content);
    }

    public String billAdjustment(String adminName, String hcfName, String invoiceNumber, String adjustmentType,
            String amount, String reason) {
        String content = """
                <h2>Bill Adjustment Created</h2>
                <p>Dear <strong>%s</strong>,</p>
                <p>A bill adjustment has been made to an HCF account.</p>

                <div class="info-box">
                    <p><strong>HCF:</strong> %s</p>
                    <p><strong>Invoice:</strong> %s</p>
                    <p><strong>Adjustment Type:</strong> %s</p>
                    <p><strong>Amount:</strong> ₹%s</p>
                    <p><strong>Reason:</strong> %s</p>
                    <p><strong>Date:</strong> %s</p>
                </div>

                <p>Please review this adjustment in the admin portal.</p>
                """.formatted(adminName, hcfName, invoiceNumber, adjustmentType, amount, reason,
                LocalDateTime.now().format(DATETIME_FORMAT));
        return wrapInTemplate("Bill Adjustment - " + invoiceNumber, content);
    }

    // ==================== AGREEMENT ====================

    public String agreementExpiryWarning(String hcfName, String agreementNumber, String expiryDate, int daysRemaining) {
        String urgencyLevel = daysRemaining <= 7 ? "URGENT" : daysRemaining <= 15 ? "IMPORTANT" : "REMINDER";
        String boxClass = daysRemaining <= 7 ? "error-box" : "warning-box";

        String content = """
                <h2>⚠️ Agreement Expiry %s</h2>
                <p>Dear <strong>%s</strong>,</p>

                <div class="%s">
                    <p><strong>Agreement Number:</strong> %s</p>
                    <p><strong>Expiry Date:</strong> %s</p>
                    <p><strong>Days Remaining:</strong> %d days</p>
                </div>

                <p>Please contact your CBWTF to renew your agreement before expiry to ensure uninterrupted service.</p>

                <h3>To Renew</h3>
                <ol>
                    <li>Contact your CBWTF administrator</li>
                    <li>Complete any pending documentation</li>
                    <li>Clear any outstanding dues</li>
                </ol>

                <p>Failure to renew may result in service suspension.</p>
                """.formatted(urgencyLevel, hcfName, boxClass, agreementNumber, expiryDate, daysRemaining);
        return wrapInTemplate("Agreement Expiry Warning - " + daysRemaining + " Days", content);
    }

    // ==================== HELPER ====================

    public String buildItemsTable(Map<String, Object>[] items) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class='table'>");
        sb.append("<tr><th>Item</th><th>Qty</th><th>Price</th><th>Total</th></tr>");
        for (Map<String, Object> item : items) {
            sb.append(String.format("<tr><td>%s</td><td>%s</td><td>₹%s</td><td>₹%s</td></tr>",
                    item.get("name"), item.get("quantity"), item.get("price"), item.get("total")));
        }
        sb.append("</table>");
        return sb.toString();
    }
}
