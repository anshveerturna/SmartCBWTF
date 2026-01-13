"use client";

import { motion } from "framer-motion";
import Image from "next/image";

export default function TrustedClients() {
  const clients = [
    {
      name: "Global Environmental Solution",
      logo: "/clients/rudra-waste-logo.png",
      width: 160, 
      height: 80,
    },
    {
      name: "Rudra Waste Management",
      logo: "/clients/global-env-sol-logo.png",
      width: 160,
      height: 80,
    },
  ];

  return (
    <div className="py-20 border-b border-[#e7e5e4] bg-white">
      <div className="container-tight text-center">
        <motion.p
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          className="text-base font-semibold text-[#047857] mb-12 uppercase tracking-wider"
        >
          Trusted by Industry Leaders
        </motion.p>
        
        <div className="flex flex-wrap justify-center items-center gap-16 lg:gap-32">
          {clients.map((client, i) => (
            <motion.div
              key={client.name}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.1 }}
              className="flex flex-col items-center gap-6 group"
            >
              <div className="relative h-24 w-48 grayscale-0 transition-transform duration-300 group-hover:scale-105">
                <Image
                  src={client.logo}
                  alt={`${client.name} logo`}
                  fill
                  className="object-contain"
                />
              </div>
              <p className="text-lg font-medium text-neutral-800 transition-opacity duration-300">
                {client.name}
              </p>
            </motion.div>
          ))}
        </div>
      </div>
    </div>
  );
}
