"use client";

import { motion } from "framer-motion";
import { useState } from "react";

type ContactFormPayload = {
  name: string;
  email: string;
  phone: string;
  organization: string;
  organizationType: string;
  inquiryType: string;
  message: string;
  website: string;
};

type FieldErrors = Partial<Record<keyof ContactFormPayload, string>>;

const organizationTypes = [
  { value: "CBWTF Operator", label: "CBWTF operator" },
  { value: "Healthcare Facility", label: "Healthcare facility" },
  { value: "Student / Researcher", label: "Student / researcher" },
  { value: "Consultant / Partner", label: "Consultant / partner" },
  { value: "Other", label: "Other" },
];

const inquiryTypes = [
  { value: "Request Demo", label: "Request a demo" },
  { value: "Pricing", label: "Pricing" },
  { value: "Platform Question", label: "Platform question" },
  { value: "Student / Research", label: "Student / research" },
  { value: "Support", label: "Support" },
  { value: "Partnership", label: "Partnership" },
  { value: "Other", label: "Other" },
];

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const phoneCharacterPattern = /^[+()\-\s0-9]+$/;

const resolveApiBaseUrl = () => {
  const configuredUrl = (process.env.NEXT_PUBLIC_API_BASE_URL || "https://api.smartcbwtf.com").replace(/\/+$/, "");
  const parsedUrl = new URL(configuredUrl);
  if (process.env.NODE_ENV === "production" && parsedUrl.protocol !== "https:") {
    throw new Error("NEXT_PUBLIC_API_BASE_URL must use HTTPS in production builds.");
  }
  return configuredUrl;
};

const API_BASE_URL = resolveApiBaseUrl();

const getTrimmedValue = (formData: FormData, key: keyof ContactFormPayload) => {
  const value = formData.get(key);
  return typeof value === "string" ? value.trim() : "";
};

const validateContactForm = (data: ContactFormPayload) => {
  const errors: FieldErrors = {};
  const phoneDigits = data.phone.replace(/\D/g, "");

  if (data.name.length < 2) {
    errors.name = "Please enter your full name.";
  }

  if (!emailPattern.test(data.email)) {
    errors.email = "Please enter a valid email address.";
  }

  if (!phoneCharacterPattern.test(data.phone) || phoneDigits.length < 10 || phoneDigits.length > 15) {
    errors.phone = "Please enter a valid phone number with 10 to 15 digits.";
  }

  if (data.organization.length < 2) {
    errors.organization = "Please enter your organization or institution.";
  }

  if (!data.organizationType) {
    errors.organizationType = "Please choose who you are contacting as.";
  }

  if (!data.inquiryType) {
    errors.inquiryType = "Please choose what your inquiry is about.";
  }

  if (data.message.length < 10) {
    errors.message = "Please add a little more detail so we can respond properly.";
  }

  return errors;
};

function FieldError({ id, message }: { id: string; message?: string }) {
  if (!message) {
    return null;
  }

  return (
    <p id={id} className="mt-2 text-sm text-red-600">
      {message}
    </p>
  );
}

export default function ContactPage() {
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);

    const formData = new FormData(e.currentTarget);
    const data: ContactFormPayload = {
      name: getTrimmedValue(formData, "name"),
      email: getTrimmedValue(formData, "email"),
      phone: getTrimmedValue(formData, "phone"),
      organization: getTrimmedValue(formData, "organization"),
      organizationType: getTrimmedValue(formData, "organizationType"),
      inquiryType: getTrimmedValue(formData, "inquiryType"),
      message: getTrimmedValue(formData, "message"),
      website: getTrimmedValue(formData, "website"),
    };

    const validationErrors = validateContactForm(data);
    setFieldErrors(validationErrors);

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    setLoading(true);

    try {
      const response = await fetch(`${API_BASE_URL}/api/public/contact`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
      });

      if (!response.ok) {
        throw new Error("Failed to send message. Please try again.");
      }

      setSubmitted(true);
    } catch (err: unknown) {
      console.error(err);
      setError("Something went wrong. Please try again later or contact us directly at info@smartcbwtf.com.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-[#FAF7F2]">
      {/* Hero */}
      <section className="pt-32 lg:pt-40 pb-20">
        <div className="container-tight">
          <div className="max-w-3xl">
            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="text-sm font-semibold text-[#047857] uppercase tracking-wider mb-4"
            >
              Contact
            </motion.p>
            <motion.h1
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="text-balance mb-6"
            >
              Let&apos;s talk
            </motion.h1>
            <motion.p
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
              className="text-xl text-[#525252] leading-relaxed"
            >
              Whether you&apos;re ready for a demo or have questions about how SmartCBWTF can help your facility, we&apos;re here to help.
            </motion.p>
          </div>
        </div>
      </section>

      {/* Contact Form & Info */}
      <section className="pb-24">
        <div className="container-tight">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-16">
            {/* Form */}
            <motion.div
              initial={{ opacity: 0, x: -30 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.3 }}
            >
              {submitted ? (
                <div className="bg-[#047857]/10 rounded-2xl p-12 text-center">
                  <div className="w-16 h-16 rounded-full bg-[#047857] flex items-center justify-center mx-auto mb-6">
                    <svg className="w-8 h-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                    </svg>
                  </div>
                  <h3 className="text-2xl font-bold text-[#1a1a1a] mb-3">Message sent!</h3>
                  <p className="text-[#525252]">
                    Thank you for reaching out. We&apos;ll get back to you within 24 hours.
                  </p>
                </div>
              ) : (
                <form onSubmit={handleSubmit} className="bg-white rounded-2xl p-8 shadow-sm" noValidate>
                  <h3 className="text-xl font-semibold text-[#1a1a1a] mb-6">Send us a message</h3>
                  
                  {error && (
                    <div className="mb-4 p-3 bg-red-50 text-red-600 text-sm rounded-lg">
                      {error}
                    </div>
                  )}

                  <div className="space-y-5">
                    {/* Row 1: Name and Email */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                      <div>
                        <label htmlFor="name" className="block text-sm font-medium text-[#1a1a1a] mb-2">
                          Name <span className="text-red-600">*</span>
                        </label>
                        <input
                          id="name"
                          type="text"
                          name="name"
                          required
                          minLength={2}
                          maxLength={120}
                          autoComplete="name"
                          aria-invalid={Boolean(fieldErrors.name)}
                          aria-describedby={fieldErrors.name ? "name-error" : undefined}
                          className="w-full px-4 py-3 bg-[#f5f0e8] border-0 rounded-lg focus:ring-2 focus:ring-[#047857] transition-all"
                          placeholder="Your name"
                        />
                        <FieldError id="name-error" message={fieldErrors.name} />
                      </div>
                      <div>
                        <label htmlFor="email" className="block text-sm font-medium text-[#1a1a1a] mb-2">
                          Email <span className="text-red-600">*</span>
                        </label>
                        <input
                          id="email"
                          type="email"
                          name="email"
                          required
                          maxLength={180}
                          autoComplete="email"
                          aria-invalid={Boolean(fieldErrors.email)}
                          aria-describedby={fieldErrors.email ? "email-error" : undefined}
                          className="w-full px-4 py-3 bg-[#f5f0e8] border-0 rounded-lg focus:ring-2 focus:ring-[#047857] transition-all"
                          placeholder="you@example.com"
                        />
                        <FieldError id="email-error" message={fieldErrors.email} />
                      </div>
                    </div>
                    
                    {/* Row 2: Phone and Organization */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                      <div>
                        <label htmlFor="phone" className="block text-sm font-medium text-[#1a1a1a] mb-2">
                          Phone <span className="text-red-600">*</span>
                        </label>
                        <input
                          id="phone"
                          type="tel"
                          name="phone"
                          required
                          inputMode="tel"
                          pattern={"^[+()\\-\\s0-9]{8,20}$"}
                          maxLength={20}
                          autoComplete="tel"
                          aria-invalid={Boolean(fieldErrors.phone)}
                          aria-describedby={fieldErrors.phone ? "phone-error" : undefined}
                          className="w-full px-4 py-3 bg-[#f5f0e8] border-0 rounded-lg focus:ring-2 focus:ring-[#047857] transition-all"
                          placeholder="+91 98765 43210"
                        />
                        <FieldError id="phone-error" message={fieldErrors.phone} />
                      </div>
                      <div>
                        <label htmlFor="organization" className="block text-sm font-medium text-[#1a1a1a] mb-2">
                          Name of the Organization <span className="text-red-600">*</span>
                        </label>
                        <input
                          id="organization"
                          type="text"
                          name="organization"
                          required
                          minLength={2}
                          maxLength={180}
                          autoComplete="organization"
                          aria-invalid={Boolean(fieldErrors.organization)}
                          aria-describedby={fieldErrors.organization ? "organization-error" : undefined}
                          className="w-full px-4 py-3 bg-[#f5f0e8] border-0 rounded-lg focus:ring-2 focus:ring-[#047857] transition-all"
                          placeholder="Facility, company, or institution"
                        />
                        <FieldError id="organization-error" message={fieldErrors.organization} />
                      </div>
                    </div>

                    {/* Row 3: Contact category and inquiry type */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                      <div>
                        <label htmlFor="organizationType" className="block text-sm font-medium text-[#1a1a1a] mb-2">
                          I am contacting as <span className="text-red-600">*</span>
                        </label>
                        <select
                          id="organizationType"
                          name="organizationType"
                          required
                          defaultValue=""
                          aria-invalid={Boolean(fieldErrors.organizationType)}
                          aria-describedby={fieldErrors.organizationType ? "organizationType-error" : undefined}
                          className="w-full px-4 py-3 bg-[#f5f0e8] border-0 rounded-lg focus:ring-2 focus:ring-[#047857] transition-all"
                        >
                          <option value="" disabled>
                            Select one
                          </option>
                          {organizationTypes.map((option) => (
                            <option key={option.value} value={option.value}>
                              {option.label}
                            </option>
                          ))}
                        </select>
                        <FieldError id="organizationType-error" message={fieldErrors.organizationType} />
                      </div>
                      <div>
                        <label htmlFor="inquiryType" className="block text-sm font-medium text-[#1a1a1a] mb-2">
                          What is this about? <span className="text-red-600">*</span>
                        </label>
                        <select
                          id="inquiryType"
                          name="inquiryType"
                          required
                          defaultValue=""
                          aria-invalid={Boolean(fieldErrors.inquiryType)}
                          aria-describedby={fieldErrors.inquiryType ? "inquiryType-error" : undefined}
                          className="w-full px-4 py-3 bg-[#f5f0e8] border-0 rounded-lg focus:ring-2 focus:ring-[#047857] transition-all"
                        >
                          <option value="" disabled>
                            Select one
                          </option>
                          {inquiryTypes.map((option) => (
                            <option key={option.value} value={option.value}>
                              {option.label}
                            </option>
                          ))}
                        </select>
                        <FieldError id="inquiryType-error" message={fieldErrors.inquiryType} />
                      </div>
                    </div>
                    
                    <div>
                      <label htmlFor="message" className="block text-sm font-medium text-[#1a1a1a] mb-2">
                        How can we help? <span className="text-red-600">*</span>
                      </label>
                      <textarea
                        id="message"
                        name="message"
                        rows={4}
                        required
                        minLength={10}
                        maxLength={2000}
                        aria-invalid={Boolean(fieldErrors.message)}
                        aria-describedby={fieldErrors.message ? "message-error" : undefined}
                        className="w-full px-4 py-3 bg-[#f5f0e8] border-0 rounded-lg focus:ring-2 focus:ring-[#047857] transition-all resize-none"
                        placeholder="Tell us about your needs..."
                      />
                      <FieldError id="message-error" message={fieldErrors.message} />
                    </div>

                    <input
                      type="text"
                      name="website"
                      tabIndex={-1}
                      autoComplete="off"
                      aria-hidden="true"
                      className="hidden"
                    />
                    
                    <button
                      type="submit"
                      disabled={loading}
                      className="w-full px-6 py-4 bg-[#047857] hover:bg-[#065f46] text-white font-semibold rounded-lg transition-all shadow-lg shadow-[#047857]/25 disabled:opacity-70 disabled:cursor-not-allowed"
                    >
                      {loading ? "Sending..." : "Send Message"}
                    </button>
                  </div>
                </form>
              )}
            </motion.div>

            {/* Contact Info */}
            <motion.div
              initial={{ opacity: 0, x: 30 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.4 }}
              className="lg:pt-12"
            >
              <div className="space-y-8">
                <div>
                  <h3 className="text-lg font-semibold text-[#1a1a1a] mb-2">Email</h3>
                  <a href="mailto:info@smartcbwtf.com" className="text-[#047857] hover:underline block mb-1">
                    info@smartcbwtf.com
                  </a>
                </div>
                
                <div>
                  <h3 className="text-lg font-semibold text-[#1a1a1a] mb-2">Response Time</h3>
                  <p className="text-[#525252]">
                    We typically respond within 24 hours during business days.
                  </p>
                </div>
              </div>
            </motion.div>
          </div>
        </div>
      </section>
    </div>
  );
}
