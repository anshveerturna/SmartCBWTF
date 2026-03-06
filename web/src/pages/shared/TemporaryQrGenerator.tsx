import React, { useState } from 'react';
import {
  Box, Card, CardContent, Typography, Button, TextField, FormControl,
  Stack, Alert, CircularProgress
} from '@mui/material';
import { QrCode as QrIcon, PictureAsPdf as PdfIcon } from '@mui/icons-material';
import QRCode from 'qrcode';
import { jsPDF } from 'jspdf';
import { v4 as uuidv4 } from 'uuid';
import CryptoJS from 'crypto-js';

// Dummy secret for local HMAC signing (not validated by DB anyway)
const DUMMY_JWT_SECRET = "dummy_secret_for_temporary_qrs";
const SIGNING_KEY = "QR_SIGN:" + DUMMY_JWT_SECRET;
const WASTE_CATEGORIES = ['YELLOW', 'RED', 'BLUE', 'WHITE'] as const;
const CATEGORY_COLORS: Record<string, number[]> = {
  YELLOW: [255, 235, 59],
  RED: [244, 67, 54],
  BLUE: [33, 150, 243],
  WHITE: [255, 255, 255],
};

function generateHmacChecksum(payloadString: string) {
  const hash = CryptoJS.HmacSHA256(payloadString, SIGNING_KEY);
  return CryptoJS.enc.Base64.stringify(hash);
}

export default function TemporaryQrGenerator() {
  const [form, setForm] = useState({
    hcfName: '',
    hcfCode: 'HCF-' + Math.floor(1000 + Math.random() * 9000),
    doctorName: '',
    contactPhone: '',
    address: '',
    validUntil: new Date(Date.now() + 30 * 86400000).toISOString().split('T')[0],
    categoryQuantities: { YELLOW: 0, RED: 0, BLUE: 0, WHITE: 0 } as Record<string, number>,
  });
  const [isGenerating, setIsGenerating] = useState(false);

  const totalLabels = Object.values(form.categoryQuantities).reduce((a, b) => a + (b || 0), 0);

  const handleGenerate = async () => {
    if (totalLabels < 1 || !form.hcfName) return;
    setIsGenerating(true);
    
    try {
      // Create a new jsPDF instance (A4 size, points as unit to match Java PDFBox)
      const doc = new jsPDF({ unit: 'pt', format: 'a4' });
      
      const margin = 20;
      const headerHeight = 80;
      const footerHeight = 40;
      const pageWidth = doc.internal.pageSize.getWidth(); // ~595.28
      const pageHeight = doc.internal.pageSize.getHeight(); // ~841.89
      const contentWidth = pageWidth - (2 * margin);
      const contentHeight = pageHeight - (2 * margin) - headerHeight - footerHeight;
      const startY = pageHeight - margin - headerHeight; // Note: jsPDF y=0 is TOP! PDFBox y=0 is BOTTOM.
      // Adjusting layout equations since jsPDF's Y-axis is inverted compared to PDFBox.
      // In jsPDF, y goes down from 0.
      const startYJsPdf = margin + headerHeight;

      const cols = 3;
      const rows = 3;
      const gap = 12;
      const labelWidth = (contentWidth - ((cols - 1) * gap)) / cols;
      const labelHeight = (contentHeight - ((rows - 1) * gap)) / rows;
      const labelsPerPage = cols * rows;

      // Mock UUIDs for this stateless generation
      const agreementId = uuidv4();
      const hcfId = uuidv4();
      const facilityId = uuidv4();
      const validFrom = new Date().toISOString();
      const validTo = new Date(form.validUntil).toISOString();

      // Collect all labels to print
      const allLabels: { cat: string, qr: string, qrId: string }[] = [];
      for (const [cat, qty] of Object.entries(form.categoryQuantities)) {
        for (let i = 0; i < qty; i++) {
          const qrId = uuidv4();
          
          const payloadObj = {
            qrId, agreementId, hcfId, facilityId,
            wasteCategory: cat, validFrom, validTo
          };
          const payloadStr = JSON.stringify(payloadObj);
          const checksum = generateHmacChecksum(payloadStr);
          
          const signedPayload = { ...payloadObj, checksum };
          const finalJson = JSON.stringify(signedPayload);
          
          allLabels.push({ cat, qr: finalJson, qrId });
        }
      }

      const numPages = Math.ceil(allLabels.length / labelsPerPage);
      const dateStr = new Date().toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });

      for (let p = 0; p < numPages; p++) {
        if (p > 0) doc.addPage();
        
        // Draw Header
        doc.setFont('helvetica', 'bold');
        doc.setFontSize(14);
        doc.setTextColor(33, 150, 243); // Primary color approx
        doc.text("SmartCBWTF", margin, margin + 20);
        
        doc.setFont('helvetica', 'normal');
        doc.setFontSize(9);
        doc.setTextColor(150, 150, 150);
        doc.text("Bio-Medical Waste Compliance Platform", margin, margin + 35);
        
        doc.setFont('helvetica', 'bold');
        doc.setFontSize(12);
        doc.setTextColor(50, 50, 50);
        doc.text("QR LABELS BATCH", pageWidth - margin, margin + 20, { align: 'right' });
        
        doc.setFont('helvetica', 'normal');
        doc.setFontSize(10);
        doc.text("Generated: " + dateStr, pageWidth - margin, margin + 35, { align: 'right' });
        
        // Header Divider
        doc.setDrawColor(200, 200, 200);
        doc.setLineWidth(0.5);
        doc.line(margin, margin + 50, pageWidth - margin, margin + 50);

        // Footer
        const footY = pageHeight - margin + 10;
        doc.line(margin, footY - 10, pageWidth - margin, footY - 10);
        doc.setFontSize(8);
        doc.setTextColor(150, 150, 150);
        doc.text("Powered by SmartCBWTF", pageWidth / 2, footY, { align: 'center' });
        doc.text(`Page ${p + 1} of ${numPages}`, pageWidth - margin, footY, { align: 'right' });

        // Draw Labels
        const startIdx = p * labelsPerPage;
        const endIdx = Math.min(startIdx + labelsPerPage, allLabels.length);

        for (let i = startIdx; i < endIdx; i++) {
          const pageIndex = i - startIdx;
          const row = Math.floor(pageIndex / cols);
          const col = pageIndex % cols;

          const x = margin + (col * (labelWidth + gap));
          const y = startYJsPdf + (row * (labelHeight + gap));

          const { cat, qr, qrId } = allLabels[i];

          // Label Outline
          doc.setDrawColor(200, 200, 200);
          doc.setLineWidth(0.5);
          doc.rect(x, y, labelWidth, labelHeight);

          // Category Header Color
          const catColor = CATEGORY_COLORS[cat as keyof typeof CATEGORY_COLORS] || [128, 128, 128];
          doc.setFillColor(catColor[0], catColor[1], catColor[2]);
          const headerH = 20;
          doc.rect(x, y, labelWidth, headerH, 'F');

          // Category Header Text
          doc.setFont('helvetica', 'bold');
          doc.setFontSize(10);
          doc.setTextColor(cat === 'WHITE' ? 0 : 255);
          doc.text(cat, x + labelWidth / 2, y + 14, { align: 'center' });

          // HCF Name
          doc.setTextColor(0, 0, 0);
          doc.setFontSize(8);
          let hcfName = form.hcfName.substring(0, 28);
          if (form.hcfName.length > 28) hcfName += "...";
          doc.text(hcfName, x + labelWidth / 2, y + 36, { align: 'center' });

          // Generate QR Code Image (DataURL)
          const qrDataUrl = await QRCode.toDataURL(qr, { width: 300, margin: 1 });
          
          // Render Bottom Details first to know QR size
          const detailFontSize = 5.5;
          const lineSp = 8;
          let currentBottomY = y + labelHeight - 5;
          doc.setFontSize(detailFontSize);

          // Valid Until
          doc.setFont('helvetica', 'bold');
          doc.setTextColor(200, 0, 0); // Darker red
          const validDate = new Date(form.validUntil + 'T00:00:00');
          const validDateStr = validDate.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
          const validText = `Valid Until: ${validDateStr}`;
          doc.text(validText, x + labelWidth / 2, currentBottomY, { align: 'center' });
          currentBottomY -= lineSp;

          // Address
          doc.setFont('helvetica', 'normal');
          doc.setTextColor(50, 50, 50);
          if (form.address) {
            doc.text(form.address.substring(0, 32), x + labelWidth / 2, currentBottomY, { align: 'center' });
            currentBottomY -= lineSp;
          }

          // Contact Phone
          if (form.contactPhone) {
            doc.text("Ph: " + form.contactPhone, x + labelWidth / 2, currentBottomY, { align: 'center' });
            currentBottomY -= lineSp;
          }

          // Doctor Name
          if (form.doctorName) {
            doc.text(form.doctorName.substring(0, 28), x + labelWidth / 2, currentBottomY, { align: 'center' });
            currentBottomY -= lineSp;
          }

          // QR Serial ID
          doc.setFont('courier', 'normal');
          doc.setFontSize(6);
          const shortCode = qrId.replace(/-/g, '').substring(qrId.length - 8).toUpperCase();
          doc.text(shortCode, x + labelWidth / 2, currentBottomY, { align: 'center' });
          currentBottomY -= lineSp;

          // Calculate QR image size
          const qrAvailableHeight = currentBottomY - (y + 38);
          let qrSize = Math.min(qrAvailableHeight, labelWidth - 16);
          if (qrSize < 40) qrSize = 40;
          
          const qrX = x + (labelWidth - qrSize) / 2;
          // In Java, image Y point is bottom-left, in jsPDF it's top-left
          const qrY = y + 42; 
          doc.addImage(qrDataUrl, 'PNG', qrX, qrY, qrSize, qrSize);
        }

        // Cut Lines
        doc.setDrawColor(200, 200, 200);
        doc.setLineWidth(0.2);
        // We'll skip complex cut lines drawing to save JS size but basically they are dashes
        doc.setLineDashPattern([3, 3], 0);
        for (let c = 1; c < cols; c++) {
          const xl = margin + (c * labelWidth) + ((c - 1) * gap) + (gap / 2);
          doc.line(xl, startYJsPdf, xl, startYJsPdf + (rows * labelHeight) + ((rows - 1) * gap));
        }
        for (let r = 1; r < rows; r++) {
          const yl = startYJsPdf + (r * labelHeight) + ((r - 1) * gap) + (gap / 2);
          doc.line(margin, yl, pageWidth - margin, yl);
        }
        doc.setLineDashPattern([], 0);
      }

      doc.autoPrint();
      window.open(doc.output('bloburl'), '_blank');
      
    } catch (e) {
      console.error(e);
      alert('Failed to generate PDF');
    } finally {
      setIsGenerating(false);
    }
  };

  const setCategoryQty = (cat: string, val: number) => {
    setForm((prev) => ({
      ...prev,
      categoryQuantities: { ...prev.categoryQuantities, [cat]: Math.max(0, Math.min(500, val)) },
    }));
  };

  return (
    <Box sx={{ p: 3, maxWidth: 800, mx: 'auto' }}>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <QrIcon sx={{ fontSize: 32, color: 'primary.main' }} />
        <Typography variant="h4" fontWeight="bold">Standalone QR Generator</Typography>
      </Box>
      <Alert severity="warning" sx={{ mb: 3 }}>
        <strong>Notice:</strong> This is a temporary, isolated QR generator. Labels generated here <strong>will not be saved</strong> to the main database and there is no tracking for them. They are purely for visual printing purposes.
      </Alert>

      <Card>
        <CardContent>
          <Stack spacing={3}>
            <TextField
              label="HCF Name"
              value={form.hcfName}
              onChange={(e) => setForm({ ...form, hcfName: e.target.value })}
              required fullWidth
            />
            <Box display="flex" gap={2}>
              <TextField
                label="HCF Code / Agreement No"
                value={form.hcfCode}
                onChange={(e) => setForm({ ...form, hcfCode: e.target.value })}
                fullWidth
              />
              <TextField
                label="Doctor / Owner Name"
                value={form.doctorName}
                onChange={(e) => setForm({ ...form, doctorName: e.target.value })}
                fullWidth
              />
            </Box>
            <Box display="flex" gap={2}>
              <TextField
                label="Contact Phone"
                value={form.contactPhone}
                onChange={(e) => setForm({ ...form, contactPhone: e.target.value })}
                fullWidth
              />
              <TextField
                label="Address"
                value={form.address}
                onChange={(e) => setForm({ ...form, address: e.target.value })}
                fullWidth
              />
            </Box>
            <TextField
              label="Valid Until"
              type="date"
              fullWidth
              value={form.validUntil}
              onChange={(e) => setForm({ ...form, validUntil: e.target.value })}
              InputLabelProps={{ shrink: true }}
            />

            <Typography variant="subtitle2" color="text.secondary" mt={2}>
              Quantity per Waste Category (Max 500 total)
            </Typography>

            {WASTE_CATEGORIES.map((cat) => (
              <Box key={cat} display="flex" alignItems="center" gap={2}>
                <Box
                  sx={{
                    width: 20, height: 20, borderRadius: '50%',
                    bgcolor: `rgb(${CATEGORY_COLORS[cat].join(',')})`, flexShrink: 0,
                    border: cat === 'WHITE' ? '1px solid #ccc' : 'none',
                  }}
                />
                <Typography sx={{ width: 70, fontWeight: 'bold' }}>{cat}</Typography>
                <TextField
                  type="number"
                  size="small"
                  value={form.categoryQuantities[cat] || 0}
                  onChange={(e) => setCategoryQty(cat, parseInt(e.target.value) || 0)}
                  inputProps={{ min: 0, max: 500, style: { textAlign: 'center' } }}
                  sx={{ width: 120 }}
                />
              </Box>
            ))}

            <Button
              variant="contained"
              size="large"
              startIcon={isGenerating ? <CircularProgress size={20} color="inherit" /> : <PdfIcon />}
              onClick={handleGenerate}
              disabled={isGenerating || totalLabels < 1 || !form.hcfName}
            >
              {isGenerating ? 'Generating PDF...' : `Generate ${totalLabels} Labels PDF`}
            </Button>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
