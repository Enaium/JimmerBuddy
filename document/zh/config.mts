import { defineAdditionalConfig } from "vitepress";

export default defineAdditionalConfig({
  lang: "zh-Hans",
  description: "JimmerBuddy 是一个为Jimmer提供一流支持的插件",
  themeConfig: {
    nav: [
      { text: "首页", link: "/zh" },
      { text: "指南", link: "/zh/guide/what-is-jimmerbuddy" },
      { text: "参考", link: "/zh/reference/feature-inspection" },
    ],
    sidebar: {
      "/zh/guide/": {
        base: "/zh/guide/",
        items: [
          {
            text: "简介",
            collapsed: false,
            items: [
              { text: "什么是JimmerBuddy", link: "what-is-jimmerbuddy" },
              { text: "快速开始", link: "quickstart" },
            ],
          },
          {
            text: "开发",
            collapsed: false,
            items: [
              { text: "生成代码", link: "generate-code" },
              { text: "DTO语言", link: "dto-language" },
            ],
          },
          {
            text: "参考",
            base: "/zh/reference/",
            link: "feature-inspection",
          },
        ],
      },
      "/zh/reference/": {
        base: "/zh/reference/",
        items: [
          {
            text: "特性",
            base: "/zh/reference/feature-",
            items: [
              { text: "检查", link: "inspection" },
              { text: "生成实体", link: "generate-entity" },
              { text: "项目向导", link: "project-wizard" },
              { text: "导航", link: "navigate" },
              { text: "SQL 到剪贴板", link: "sql-to-clipboard" },
              { text: "内联提示", link: "inlay-hit" },
              { text: "后缀模板", link: "postfix-template" },
            ],
          },
          {
            text: "DTO",
            base: "/zh/reference/dto-",
            items: [
              { text: "语法高亮", link: "syntax-highlight" },
              { text: "语法检查", link: "syntax-check" },
              { text: "原生编译器检查", link: "native-compiler-check" },
              { text: "补全", link: "completion" },
              { text: "导航", link: "navigation" },
              { text: "自动导入", link: "automatically-import" },
              { text: "格式化代码", link: "format-code" },
              { text: "结构视图", link: "structure-view" },
              { text: "可视化创建", link: "visualization-create" },
            ],
          },
        ],
      },
    },
    footer: {
      message: "基于 Apache 许可发布",
      copyright: `版权所有 © 2025-${new Date().getFullYear()} Enaium`,
    },
    docFooter: {
      prev: "上一页",
      next: "下一页",
    },
    outline: {
      label: "页面导航",
    },
    returnToTopLabel: "回到顶部",
    darkModeSwitchLabel: "主题",
    lightModeSwitchTitle: "切换到浅色模式",
    darkModeSwitchTitle: "切换到深色模式",
  },
});
