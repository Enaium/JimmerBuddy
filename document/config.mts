import { defineAdditionalConfig } from "vitepress";

export default defineAdditionalConfig({
  lang: "en-US",
  description: "A plugin that adds first-class support for Project Jimmer",
  themeConfig: {
    nav: [
      { text: "Home", link: "/" },
      { text: "Guide", link: "/guide/what-is-jimmerbuddy" },
      { text: "Reference", link: "/reference/feature-inspection" },
      { text: "Setting", link: "/setting/setting-icon" },
    ],
    sidebar: {
      "/guide/": {
        base: "/guide/",
        items: [
          {
            text: "Introduction",
            collapsed: false,
            items: [
              { text: "What is JimmerBuddy", link: "what-is-jimmerbuddy" },
              { text: "Quickstart", link: "quickstart" },
            ],
          },
          {
            text: "Development",
            collapsed: false,
            items: [
              { text: "Generate Code", link: "generate-code" },
              { text: "DTO Lanauge", link: "dto-language" },
            ],
          },
          {
            text: "Reference",
            base: "/reference/",
            link: "feature-inspection",
          },
          {
            text: "Setting",
            base: "/setting/",
            link: "setting-icon",
          },
        ],
      },
      "/reference/": {
        base: "/reference/",
        items: [
          {
            text: "Feature",
            base: "/reference/feature-",
            items: [
              { text: "Inspection", link: "inspection" },
              { text: "Generate Entity", link: "generate-entity" },
              { text: "Project Wizard", link: "project-wizard" },
              { text: "Navigate", link: "navigate" },
              { text: "SQL to Clipboard", link: "sql-to-clipboard" },
              { text: "Inlay Hit", link: "inlay-hit" },
              { text: "Postfix Template", link: "postfix-template" },
            ],
          },
          {
            text: "DTO",
            base: "/reference/dto-",
            items: [
              { text: "Syntax Highlight", link: "syntax-highlight" },
              { text: "Syntax Check", link: "syntax-check" },
              { text: "Native Compiler Check", link: "native-compiler-check" },
              { text: "Completion", link: "completion" },
              { text: "Navigation", link: "navigation" },
              { text: "Automatically import", link: "automatically-import" },
              { text: "Format Code", link: "format-code" },
              { text: "Structure View", link: "structure-view" },
              { text: "Visualization Create", link: "visualization-create" },
              { text: "Hover", link: "hover" },
            ],
          },
        ],
      },
      "/setting/": {
        base: "/setting/",
        items: [
          {
            text: "Icon",
            link: "setting-icon",
          },
          {
            text: "Auto Generate",
            link: "setting-auto-generate",
          },
          {
            text: "Generate When Startup",
            link: "setting-generate-when-startup",
          },
        ],
      },
    },
    footer: {
      message: "Released under the Apache License.",
      copyright: `Copyright © 2025-${new Date().getFullYear()} Enaium`,
    },
  },
});
