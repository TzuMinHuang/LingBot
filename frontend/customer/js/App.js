import { ChatApp } from './ChatApp.js';

document.addEventListener('DOMContentLoaded', () => {

  const chatbotToggler = document.querySelector(".chatbot-toggler");
  const chatbotCloseBtn = document.querySelector(".chatbot-close-btn");

  chatbotToggler.addEventListener("click", () => {
  document.body.classList.toggle("show-chatbot");

  });
  chatbotCloseBtn.addEventListener("click", () => document.body.classList.remove("show-chatbot"));
  
  const app = new ChatApp("http://localhost");
});