from pathlib import Path

from docx import Document
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Pt


PROJECT_DOCS = Path("docs/project")


def set_font(run, size=11, bold=False):
    run.font.name = "Calibri"
    run._element.rPr.rFonts.set(qn("w:ascii"), "Calibri")
    run._element.rPr.rFonts.set(qn("w:hAnsi"), "Calibri")
    run._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")
    run.font.size = Pt(size)
    run.bold = bold


def shade(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), fill)
    tc_pr.append(shd)


def set_width(cell, value):
    tc_pr = cell._tc.get_or_add_tcPr()
    width = tc_pr.first_child_found_in("w:tcW")
    if width is None:
        width = OxmlElement("w:tcW")
        tc_pr.append(width)
    width.set(qn("w:w"), str(value))
    width.set(qn("w:type"), "dxa")


def append_heading(doc, text):
    doc.add_paragraph(text, style="Heading 1")


def append_note(doc, text):
    paragraph = doc.add_paragraph()
    paragraph.paragraph_format.space_after = Pt(8)
    run = paragraph.add_run(text)
    set_font(run, 10, False)


def append_bullets(doc, items):
    for item in items:
        paragraph = doc.add_paragraph(style="List Bullet")
        paragraph.paragraph_format.space_after = Pt(4)
        run = paragraph.add_run(item)
        set_font(run)


def append_table(doc, headers, rows, widths):
    table = doc.add_table(rows=1, cols=len(headers))
    for index, header in enumerate(headers):
        cell = table.rows[0].cells[index]
        cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
        shade(cell, "E8EEF5")
        paragraph = cell.paragraphs[0]
        paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
        run = paragraph.add_run(header)
        set_font(run, 9.5, True)
    for row in rows:
        cells = table.add_row().cells
        for index, value in enumerate(row):
            cell = cells[index]
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            paragraph = cell.paragraphs[0]
            run = paragraph.add_run(value)
            set_font(run, 9.5)
    for row in table.rows:
        for index, cell in enumerate(row.cells):
            set_width(cell, widths[index])
            for paragraph in cell.paragraphs:
                paragraph.paragraph_format.space_after = Pt(3)
                paragraph.paragraph_format.line_spacing = 1.15


def update_prd():
    path = PROJECT_DOCS / "01-EventFlow产品需求文档.docx"
    doc = Document(path)
    append_heading(doc, "7. 已确认的 MVP 规则")
    append_note(doc, "修订说明：以下规则已由项目负责人确认，作为后续 UI、接口、数据模型和测试的共同基线。")
    append_table(doc, ["规则", "确认结果"], [
        ["报名人工审核", "MVP 不启用人工审核；用户确认后直接成为 CONFIRMED。审核型报名作为后续扩展，不引入当前状态机。"],
        ["活动发布", "组织者可直接发布；平台管理员保留活动下线和异常处置权限。"],
        ["候补保留时长", "候补递补成功后获得 10 分钟 RESERVED 保留期；超时后继续递补下一位。"],
        ["取消规则", "已确认报名可在活动开始前 24 小时主动取消；超过该时间仅管理员可按异常流程处理。"],
        ["登录方式", "MVP 使用账号密码登录；开发和演示环境允许固定验证码模拟登录，不接入真实短信。"],
        ["报名信息", "固定采集姓名、手机号、邮箱、公司或机构、职位；不做动态表单。"],
        ["二维码签到", "使用短时有效的动态二维码；签到接口幂等，同一报名记录只能生成一条有效签到记录。"],
        ["名额统计", "RESERVED 与 CONFIRMED 均占用名额；WAITING、CANCELLED 与 EXPIRED 不占用名额。"],
        ["并发基线", "首期以单场次 1,000 个并发报名请求为压测基线，验收不得出现超卖或重复报名。"],
    ], [2250, 7110])
    doc.save(path)


def update_workflow():
    path = PROJECT_DOCS / "02-EventFlow核心流程与状态机.docx"
    doc = Document(path)
    append_heading(doc, "6. 已冻结的流程边界")
    append_note(doc, "修订说明：本节解决 MVP 中可能产生额外状态或流程分支的歧义。")
    append_bullets(doc, [
        "报名不经过人工审核，因此报名主链路不引入 PENDING_REVIEW 状态；RESERVED 确认后直接转换为 CONFIRMED。",
        "活动由组织者直接发布。管理员可将异常、违规或需停止报名的活动转为 OFFLINE，并记录操作日志。",
        "用户仅可在活动开始前 24 小时取消已确认报名；取消或 RESERVED 过期都必须释放名额并触发候补递补。",
        "候补递补的 RESERVED 保留期固定为 10 分钟；候补用户取消、超时或确认后均需以条件更新保证状态唯一转移。",
        "签到二维码必须短时有效，服务端校验报名状态、二维码有效期和签到幂等性；只有 CONFIRMED 用户可进入 CHECKED_IN。",
    ])
    doc.save(path)


def update_testing():
    path = PROJECT_DOCS / "04-EventFlow测试与验收方案.docx"
    doc = Document(path)
    append_heading(doc, "5. 已确认的压测与验收基线")
    append_note(doc, "修订说明：以下项目是 MVP 上线前必须验证的不可变约束。")
    append_table(doc, ["验收项", "通过标准"], [
        ["并发报名", "单场次 1,000 个并发报名请求下，系统无超卖、无重复报名，报名结果可查询。"],
        ["名额守恒", "任一时刻：可用名额 + RESERVED 数量 + CONFIRMED 数量 = 场次总容量。"],
        ["取消窗口", "活动开始前 24 小时内可取消；超过窗口普通用户接口被拒绝，管理员异常操作留痕。"],
        ["候补递补", "释放名额后仅最早有效候补获得 10 分钟保留资格；并发释放时不出现重复递补。"],
        ["签到幂等", "动态二维码过期或非 CONFIRMED 用户不可签到；同一报名重复扫码只保留一条签到记录。"],
    ], [2450, 6910])
    doc.save(path)


def main():
    update_prd()
    update_workflow()
    update_testing()


if __name__ == "__main__":
    main()
