<?xml version="1.0" encoding="windows-1251" ?>
<xsl:stylesheet version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:dyn="http://exslt.org/dynamic"
  xmlns:courier="xalan://ru.rd.courier.xalan.XalanFuns"
  xmlns:courier-ae="xalan://ru.rd.courier.xalan.ApplyExcept"
  extension-element-prefixes="courier-ae"
  exclude-result-prefixes="xalan dyn courier courier-ae"
>

  <xsl:param name="app-dir"/>

  <xsl:template match="@*|node()|text()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <!--xsl:template name="sybase">
    <xsl:param name="ctx"/>
    <xsl:variable name="ct">
      <xsl:choose>
        <xsl:when test="$ctx"><xsl:copy-of select="$ctx"/></xsl:when>
        <xsl:otherwise><xsl:copy-of select="."/></xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="c" select="xalan:nodeset($ct)/*"/>
    
    <xsl:message><xsl:value-of select="concat(name(), '|', courier:hasWord(name(), 'host, sybase, port, db'))"/></xsl:message>
    

    <database
      url="jdbc:sybase:Tds:{$c/@host}:{$c/@port}/{$c/@db}"
      driver="com.sybase.jdbc2.jdbc.SybDriver"
      check-sql="SELECT GetDate()"
    >
      <xsl:apply-templates select="$c/@*[not(courier:hasWord(name(), 'host,port,db'))]|$c/node()"/>
    </database>
  </xsl:template>

  <xsl:template match="sybase">
    <xsl:call-template name="sybase"/>
  </xsl:template>

  <xsl:template match="sybase2">
    <xsl:call-template name="sybase">
      <xsl:with-param name="ctx">
        <sybase name="test-sybase2" host="test-host2" port="88882" db="test-db2" username="test-user2" password="test-pwd2"/>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template-->


  <xsl:template name="sybase">
    <xsl:param name="ctx"/>
    <xsl:variable name="c" select="courier:ctx()"/>
    
    <!--xsl:message><xsl:value-of select="courier:findSingleFile('.', '^bond.*')"/></xsl:message>
    <xsl:message><xsl:value-of select="concat(name(), '|', name($c), '|', count($c), '|', courier:hasWord(name($c), 'host, sybase, port, db'))"/></xsl:message-->

    <database
      url="jdbc:sybase:Tds:{$c/@host}:{$c/@port}/{$c/@db}"
      driver="com.sybase.jdbc2.jdbc.SybDriver"
      check-sql="SELECT GetDate()"
    >
      <!--xsl:apply-templates select="$c/@*[not(name() = 'host' or name() = 'port' or name() = 'db')]|$c/node()"/-->
      
      <!--xsl:apply-templates select="$c/@*[not(courier:hasWord(name(), 'host,port,db'))]|$c/node()"/-->

      <!--xsl:variable name="an" select="courier-ae:createAttrs('host,port,db')"/>
      <xsl:apply-templates select="courier-ae:exclude($an, $c)"/-->

      <xsl:attribute name="aaaaaaaa">vvvvvv</xsl:attribute>
      
      <xsl:apply-templates select="courier:filter($c, '', 'host,port,db')"/>


    </database>
  </xsl:template>

  <xsl:template match="sybase">
    <xsl:call-template name="sybase"/>
  </xsl:template>

  <xsl:template match="sybase2">
    <xsl:call-template name="sybase">
      <xsl:with-param name="ctx">
        <sybase name="test-sybase2" host="test-host2" port="88882" db="test-db2" username="test-user2" password="test-pwd2"/>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>


</xsl:stylesheet>