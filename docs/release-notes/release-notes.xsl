<?xml version="1.0" encoding="windows-1251" ?>
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>

<xsl:template match="@*|node()|text()">
  <xsl:copy>
    <xsl:apply-templates select="@*|node()|text()"/>
  </xsl:copy>
</xsl:template>
  
<xsl:template match="/">
  <head>
    <style>
TD {
  font-family: Courier; 
  font-size: 16px
}
    </style>
  </head>
  <body>
    <table width="100%" cellspacing="0" cellpadding="0">
      <xsl:apply-templates/>
    </table>
  </body>
</xsl:template>

<xsl:template match="version">
  <tr><td bgcolor="#E0E0FF" style="padding: 4px">Version <xsl:value-of select="@number"/> (<xsl:value-of select="@date"/>)</td></tr>
  <tr><td><table width="100%" cellspacing="0" cellpadding="0">
    <tr>
      <td>&#160;</td>
      <td width="100%">
        <table width="100%" cellspacing="0" cellpadding="4">
          <xsl:apply-templates select="features"/>
          <xsl:apply-templates select="bug-fixes"/>
        </table>
      </td>
    </tr>
  </table></td></tr>
</xsl:template>

<xsl:template match="features">
  <tr bgcolor="#E0FFE0"><td colspan="2">New features:</td></tr>
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="bug-fixes">
  <tr bgcolor="#E0FFE0"><td colspan="2">Fixed defects:</td></tr>
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="feature">
  <tr>
    <td valign="top">&#160;-</td>
    <td width="100%">
      <xsl:attribute name="style">
        <xsl:if test="position() != 1">border-top: 1px dotted #A0A0A0</xsl:if>
      </xsl:attribute>
      <xsl:apply-templates/>
      <!--xsl:value-of select="." disable-output-escaping = "yes"/-->
      <xsl:if test="@doc"> (see <font color="#000080"><xsl:value-of select="@doc"/></font>)</xsl:if>
    </td>
  </tr>
</xsl:template>

<xsl:template match="bug-fix">
  <tr>
    <td valign="top">&#160;-</td>
    <td width="100%">
      <xsl:attribute name="style">
        <xsl:if test="position() != 1">border-top: 1px dotted #A0A0A0</xsl:if>
      </xsl:attribute>
      <xsl:value-of select="." disable-output-escaping = "yes"/>
      <xsl:if test="@doc"> (see <font color="#000080"><xsl:value-of select="@doc"/></font>)</xsl:if>
    </td>
  </tr>
</xsl:template>

<xsl:template match="list">
  <table cellspacing="0" cellpadding="1">
    <xsl:apply-templates/>
  </table>
</xsl:template>

<xsl:template match="list/item">
  <tr>
    <td>&#160;</td>
    <td>&#187;</td>
    <td><xsl:apply-templates/></td>
  </tr>
</xsl:template>


</xsl:stylesheet>