<?xml version="1.0" encoding="windows-1251" ?>
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:dyn="http://exslt.org/dynamic"
  xmlns:java="http://xml.apache.org/xalan/java"
  xmlns:courier="xalan://ru.rd.courier.xalan.XalanFuns"
  exclude-result-prefixes="xalan dyn java courier"
>
<xsl:param name="system-conf"/>
<xsl:param name="app-dir"/>
<xsl:output method="xml" indent="yes"/>
<xsl:preserve-space elements="*"/>

<xsl:variable name="system-params" select="$system-conf//scripting"/>

<xsl:variable name="rs-counter-var-name" select="$system-params/@rs-counter-var-name"/>
<xsl:variable name="portion-exceed-message" select="'Source portion size exceeded: '"/>

<xsl:variable name="target-profile-object-name" select="$system-params/@target-profile-object-name"/>
<xsl:variable name="target-profile-object-data-stmt" select="$system-params/@target-profile-object-data-stmt"/>

<xsl:variable name="source-iterator-object-name" select="$system-params/@source-iterator-object-name"/>
<xsl:variable name="source-main-stmt-name" select="$system-params/@source-main-stmt-name"/>
<xsl:variable name="source-before-stmt-name" select="$system-params/@source-before-stmt-name"/>
<xsl:variable name="source-after-stmt-name" select="$system-params/@source-after-stmt-name"/>
<xsl:variable name="source-finally-stmt-name" select="$system-params/@source-finally-stmt-name"/>

<xsl:variable name="source-db-name" select="$system-params/@source-db-name"/>
<xsl:variable name="target-db-name" select="$system-params/@target-db-name"/>
<xsl:variable name="top-label" select="$system-params/@top-label"/>
<xsl:variable name="records-buffer-limit-var" select="$system-params/@records-buffer-limit-var"/>
<xsl:variable name="bytes-buffer-limit-var" select="$system-params/@bytes-buffer-limit-var"/>
<xsl:variable name="break-var" select="$system-params/@break-var"/>
<xsl:variable name="std-update-operation-tag" select="$system-params/@std-update-operation-tag"/>
<xsl:variable name="portion-size-var" select="$system-params/@portion-size-var"/>
<xsl:variable name="error-var" select="'$error'"/>
<xsl:variable name="last-source-record-var" select="'$last-source-record-var'"/>

<xsl:variable name="terminate-if-rs-name" select="'terminate-if-result-set'"/>

<xsl:template match="@*|node()">
  <xsl:copy>
    <xsl:apply-templates select="@*|node()"/>
  </xsl:copy>
</xsl:template>

<!-- ******************************************************************************** -->
<xsl:template match="source-profiles//rule">
  <xsl:copy>
    <xsl:attribute name="interval-step"><xsl:value-of select="transform/portion/@step"/></xsl:attribute>
    <xsl:apply-templates select="@*|node()"/>
  </xsl:copy>
</xsl:template>
<!-- ******************************************************************************** -->

<xsl:template match="pipelines-config">
  <xsl:variable name="pms">
    <xsl:choose>
      <xsl:when test="@script-mode = 'yes'">
        <catch-error rethrow="yes" suppress-logging="yes">
          <execute>
            <xsl:call-template name="call-script">
              <xsl:with-param name="script-name" select="'pipes'"/>
            </xsl:call-template>
          </execute>
          <catch>
            <stop-courier timeout="0" exit-status="1"/>      
          </catch>
        </catch-error>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="call-script">
          <xsl:with-param name="script-name" select="'pipes'"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
    
  <xsl:copy>
    <xsl:apply-templates select="@*|node()"/>
    <pipes-main-statement>
      <xsl:apply-templates select="xalan:nodeset($pms)/*"/>
    </pipes-main-statement>
    <source-main-statement><xsl:call-template name="source-main-statement"/></source-main-statement>
  </xsl:copy>
</xsl:template>

<xsl:template match="db-profiles">
  <xsl:variable name="null_db" select="*[@name='$null']"/>
  <xsl:variable name="ds_db" select="*[@name='$file-system']"/>
  
  <xsl:copy>
    <xsl:apply-templates select="@*|node()"/>
    <xsl:if test="not(null_db)">
      <source name="$null" type="null"/>
    </xsl:if>
    <xsl:if test="not(fs_db)">
      <source name="$file-system" type="file-system" show-exec-output="yes"/>
    </xsl:if>
  </xsl:copy>
</xsl:template>

<xsl:template name="call-statement">
  <xsl:param name="obj-name"/>
  <xsl:param name="stmt-name"/>

  <object-stmt-call>
    <string><xsl:value-of select="dyn:evaluate(concat('$', $obj-name, '-object-name'))"/></string>
    <string><xsl:value-of select="dyn:evaluate(concat('$', $stmt-name, '-stmt-name'))"/></string>
  </object-stmt-call>
</xsl:template>

<xsl:template name="call-source-statement">
  <xsl:param name="stmt-name"/>

  <xsl:call-template name="call-statement">
    <xsl:with-param name="obj-name" select="'source-iterator'"/>
    <xsl:with-param name="stmt-name" select="concat('source-', $stmt-name)"/>
  </xsl:call-template>
</xsl:template>

<xsl:template name="call-target-stmt">
  <xsl:param name="stmt-name"/>

  <object-stmt-call>
    <string><xsl:value-of select="$target-profile-object-name"/></string>
    <string><xsl:value-of select="$stmt-name"/></string>
  </object-stmt-call>
</xsl:template>

<xsl:template match="base-process">
  <xsl:copy>
    <xsl:apply-templates select="@*|node()"/>
    <xsl:if test="not(script)">
      <script>
        <xsl:call-template name="call-std-script"/>
      </script>
    </xsl:if>
  </xsl:copy>
</xsl:template>

<xsl:template name="call-script">
  <xsl:param name="script-name"/>
  
  <object-stmt-call>
    <string><xsl:value-of select="'Scripts'"/></string>
    <string><xsl:value-of select="$script-name"/></string>
  </object-stmt-call>
</xsl:template>

<xsl:template name="call-std-script">
  <xsl:call-template name="call-script">
    <xsl:with-param name="script-name" select="'std'"/>
  </xsl:call-template>
</xsl:template>

<xsl:template match="base-process/script//exec-process | base-process/script//inner-script | launch-process/script//exec-process | launch-process/script//inner-script">
  <xsl:call-template name="call-std-script"/>
</xsl:template>

<xsl:template match="pipelines/script//inner-script">
  <xsl:call-template name="call-script">
    <xsl:with-param name="script-name" select="'pipe'"/>
  </xsl:call-template>
</xsl:template>

<xsl:template match="pipeline/script//inner-script">
  <xsl:call-template name="call-script">
    <xsl:with-param name="script-name" select="'process'"/>
  </xsl:call-template>
</xsl:template>

<xsl:template match="/*/start-script | pipelines/script | pipeline/script | base-process/script">
  <xsl:copy>
    <xsl:call-template name="block-helper">
      <xsl:with-param name="block-content" select="."/>
    </xsl:call-template>
  </xsl:copy>
</xsl:template>

<xsl:template name="source-main-statement">
  <block><array>
    <set-var>
      <string><xsl:value-of select="$error-var"/></string>
      <null-string/>
    </set-var>
    <finally>
      <catch>
        <block>
          <string><xsl:value-of select="$top-label"/></string>
          <array>
            <xsl:call-template name="call-source-statement">
              <xsl:with-param name="stmt-name" select="'before'"/>
            </xsl:call-template>
            
            <xsl:call-template name="call-target-stmt">
              <xsl:with-param name="stmt-name" select="'before'"/>
            </xsl:call-template>

            <xsl:call-template name="test-break-var"/> <!-- just in case because var-query tests break var -->
            <target-buffer>
              <string><xsl:value-of select="$target-db-name"/></string>
              <string><xsl:value-of select="$target-profile-object-name"/></string>
              <string><xsl:value-of select="$records-buffer-limit-var"/></string>
              <string><xsl:value-of select="$bytes-buffer-limit-var"/></string>
              <string><xsl:value-of select="$system-params/@target-portion-var"/></string>
              <string><xsl:value-of select="$portion-size-var"/></string>
              <xsl:call-template name="call-source-statement">
                <xsl:with-param name="stmt-name" select="'main'"/>
              </xsl:call-template>
            </target-buffer>
            <xsl:call-template name="call-target-stmt">
              <xsl:with-param name="stmt-name" select="'after'"/>
            </xsl:call-template>

            <xsl:call-template name="call-source-statement">
              <xsl:with-param name="stmt-name" select="'after'"/>
            </xsl:call-template>
          </array>
        </block>
        <nothing/>
        <true/>
        <not><true/></not>
        <string><xsl:value-of select="$error-var"/></string>
      </catch>

      <xsl:call-template name="call-source-statement">
        <xsl:with-param name="stmt-name" select="'finally'"/>
      </xsl:call-template>
    </finally>
  </array></block>
</xsl:template>

<xsl:template name="check-max-portion-size">
<xsl:if test="portion[1]/@max-portion-size">
  <if>
    <int-greater-const>
      <var-value><string><xsl:value-of select="$rs-counter-var-name"/></string></var-value>
      <int><xsl:value-of select="portion[1]/@max-portion-size"/></int>
    </int-greater-const>
    <warning><template><string><xsl:value-of
      select="$portion-exceed-message"/>[%<xsl:value-of select="$rs-counter-var-name"/>]</string>
    </template></warning>
  </if>
</xsl:if>
</xsl:template>

<xsl:template name="process-record-statement">
  <block><array>
    <xsl:apply-templates select="record-operations/node()"/>
    
    <target-operations-wrapper>
      <block><array>
        <xsl:call-template name="call-target-stmt">
          <xsl:with-param name="stmt-name" select="$target-profile-object-data-stmt"/>
        </xsl:call-template>

        <inc-receiver-records receiver-name="{$target-db-name}"/>
        <if>
          <test-var>
            <string><xsl:value-of select="$last-source-record-var"/></string>
            <string>1</string>
          </test-var>
          <flush>
            <string><xsl:value-of select="$target-db-name"/></string>
          </flush>
        </if>
      </array></block>
    </target-operations-wrapper>
    
    <xsl:call-template name="test-break-var"/>
  </array></block>
</xsl:template>

<xsl:template name="data-query">
  <xsl:param name="add-sync-specifics" select="'y'"/>
  <xsl:param name="portion" select="'y'"/>
  <xsl:param name="request-string"><xsl:value-of select="data-query"/></xsl:param>

  <xsl:if test="$portion = 'y'">
    <xsl:apply-templates select="portion/before/*"/>
  </xsl:if>

  <rs-loop>
    <template>
      <xsl:call-template name="source-db-name-helper">
        <xsl:with-param name="operation-tag" select="data-query"/>
      </xsl:call-template>
    </template>
    <string><xsl:value-of select="$system-params/@data-rs-name"/></string>
    <template><string><xsl:value-of select="$request-string"/></string></template>
    <xsl:if test="$add-sync-specifics = 'y'">
      <xsl:call-template name="process-record-statement"/>
    </xsl:if>
    <string></string>
    <string><xsl:value-of select="$rs-counter-var-name"/></string>
    <string><xsl:value-of select="$last-source-record-var"/></string>
    <boolean>
      <xsl:choose>
        <xsl:when test="data-query/@error-as-null = 'yes'">true</xsl:when>
        <xsl:otherwise>false</xsl:otherwise>
      </xsl:choose>      
    </boolean>
    <xsl:choose>
      <xsl:when test="data-query/@prefix">
        <template><string><xsl:value-of select="data-query/@prefix"/></string></template>
      </xsl:when>
      <xsl:otherwise>
        <null-string/>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="data-query/@xml-var">
        <string><xsl:value-of select="data-query/@xml-var"/></string>
      </xsl:when>
      <xsl:otherwise>
        <string null="yes"/>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="data-query/@xml-record-tag">
        <string><xsl:value-of select="data-query/@xml-record-tag"/></string>
      </xsl:when>
      <xsl:otherwise>
        <string null="yes"/>
      </xsl:otherwise>
    </xsl:choose>
    <boolean>
      <xsl:choose>
        <xsl:when test="data-query/@xml-attributes = 'yes'">true</xsl:when>
        <xsl:otherwise>false</xsl:otherwise>
      </xsl:choose>      
    </boolean>
  </rs-loop>

  <xsl:if test="$portion = 'y'">
    <flush>
      <string><xsl:value-of select="$target-db-name"/></string>
    </flush>
    <xsl:call-template name="check-max-portion-size"/>
    <xsl:apply-templates select="portion/after/*"/>
  </xsl:if>
</xsl:template>

<xsl:template name="get-query-db-name">
  <xsl:param name="query-tag"/>
  <xsl:param name="default-name" select="$source-db-name"/>

  <string>
    <xsl:choose>
      <xsl:when test="$query-tag/@db-name">
        <xsl:value-of select="$query-tag/@db-name"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$default-name"/>
      </xsl:otherwise>
    </xsl:choose>
  </string>
</xsl:template>

<xsl:template name="source-data-iterator">
  <xsl:variable name="type" select="portion[1]/@type"/>
  <xsl:choose>
    <xsl:when test="$type = 'list'">
      <rs-loop>
        <template>
          <xsl:call-template name="get-query-db-name">
            <xsl:with-param name="query-tag" select="list-query[1]"/>
          </xsl:call-template>
        </template>
        <string null="yes"/>
        <template><string><xsl:value-of select="list-query[1]"/></string></template>
        <block><array>
          <xsl:call-template name="data-query"/>
        </array></block>
        <string null="yes"/>
        <string null="yes"/>
        <string null="yes"/>
        <boolean>
          <xsl:choose>
            <xsl:when test="list-query[1]/@error-as-null = 'yes'">true</xsl:when>
            <xsl:otherwise>false</xsl:otherwise>
          </xsl:choose>      
        </boolean>
        <xsl:if test="list-query[1]/@prefix">
          <template><string><xsl:value-of select="list-query[1]/@prefix"/></string></template>
        </xsl:if>
      </rs-loop>
    </xsl:when>
    <xsl:when test="$type = 'interval'">
      <block><array>
        <while>
          <true/>
          <block><array>
            <xsl:call-template name="data-query"/>
            <if>
              <test-var>
                <string><xsl:value-of select="$rs-counter-var-name"/></string>
                <string>0</string>
              </test-var>
              <break><string><xsl:value-of select="$top-label"/></string></break>
            </if>
          </array></block>
        </while>
      </array></block>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="data-query">
        <xsl:with-param name="portion" select="n"/>
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="source-profiles//transform">
  <statements>
    <before>
      <xsl:call-template name="block-helper">
        <xsl:with-param name="block-content" select="before"/>
      </xsl:call-template>
    </before>
    <main><xsl:call-template name="source-data-iterator"/></main>
    <after>
      <xsl:call-template name="block-helper">
        <xsl:with-param name="block-content" select="after"/>
      </xsl:call-template>
    </after>
    <finally>
      <xsl:call-template name="block-helper">
        <xsl:with-param name="block-content" select="finally"/>
      </xsl:call-template>
    </finally>
  </statements>
  <xsl:call-template name="db-links">
    <xsl:with-param name="script-node" select="."/>
  </xsl:call-template>
</xsl:template>

<xsl:template match="target-profiles/profile/after | target-profiles/profile/before | target-profiles//portion/before-script | target-profiles//portion/after-script">
  <xsl:copy>
    <xsl:apply-templates select="@*"/>
    <xsl:call-template name="block-helper">
      <xsl:with-param name="block-content" select="."/>
    </xsl:call-template>
  </xsl:copy>
</xsl:template>

<!--
<xsl:template match="target-profiles/profile/portion/before | target-profiles/profile/portion/after">
  <xsl:copy>
    <template>
      <xsl:choose>
        <xsl:when test="@xml-as-text='yes'">
          <node><xsl:copy-of select="./node() | ./text()"/></node>
        </xsl:when>
        <xsl:otherwise>
          <string><xsl:value-of select="."/></string>
        </xsl:otherwise>
      </xsl:choose>
    </template>
  </xsl:copy>
</xsl:template>
-->

<xsl:template match="target-profiles//rules">
  <transform>
    <xsl:choose>
      <xsl:when test="@var">
        <case>
          <var-value><string><xsl:value-of select="@var"/></string></var-value>
          <map>
            <xsl:for-each select="rule">
              <xsl:element name="entry">
                <xsl:attribute name="key"><xsl:value-of select="@value"/></xsl:attribute>
                <xsl:call-template name="block-helper">
                  <xsl:with-param name="block-content" select="."/>
                </xsl:call-template>
              </xsl:element>
            </xsl:for-each>
          </map>
          <xsl:if test="else">
            <xsl:call-template name="block-helper">
              <xsl:with-param name="block-content" select="else"/>
            </xsl:call-template>
          </xsl:if>
        </case>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="block-helper">
          <xsl:with-param name="block-content" select="rule[1]"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </transform>
  <xsl:call-template name="db-links">
    <xsl:with-param name="script-node" select=". | ../before | ../after | ../portion/before-script"/>
  </xsl:call-template>
</xsl:template>

<xsl:template name="db-links">
  <xsl:param name="script-node"/>

  <db-links>
    <xsl:for-each select="$script-node//*[@db-name]">
      <xsl:element name="db">
        <xsl:attribute name="type">
          <xsl:choose>
            <xsl:when test="name() = 'operation'">receiver</xsl:when>
            <xsl:otherwise>source</xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
        <xsl:value-of select="@db-name"/></xsl:element>
    </xsl:for-each>
  </db-links>
</xsl:template>

<xsl:template name="block-helper">
  <xsl:param name="block-content"/>

  <xsl:choose>
    <xsl:when test="count($block-content/*)=0">
      <nothing/>
    </xsl:when>
<!--
    <xsl:when test="count($block-content/*)=1">
      <xsl:apply-templates select="$block-content/*"/>
    </xsl:when>
-->
    <xsl:otherwise>
      <block><array><xsl:apply-templates select="$block-content/*"/></array></block>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="db-name-attribute-helper">
  <xsl:param name="operation-tag" select="."/>
  <xsl:param name="default-name"/>

  <xsl:choose>
    <xsl:when test="$operation-tag/@db-name">
      <template><string><xsl:value-of select="$operation-tag/@db-name"/></string></template>
    </xsl:when>
    <xsl:otherwise>
      <string><xsl:value-of select="$default-name"/></string>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="source-db-name-helper">
  <xsl:param name="operation-tag" select="."/>

  <xsl:call-template name="db-name-attribute-helper">
    <xsl:with-param name="operation-tag" select="$operation-tag"/>
    <xsl:with-param name="default-name" select="$source-db-name"/>
  </xsl:call-template>
</xsl:template>

<xsl:template name="target-db-name-helper">
  <xsl:call-template name="db-name-attribute-helper">
    <xsl:with-param name="operation-tag" select="."/>
    <xsl:with-param name="default-name" select="$target-db-name"/>
  </xsl:call-template>
</xsl:template>

<!-- *************************************************************************** -->
<!-- ****************************** Operations ********************************* -->
<!-- *************************************************************************** -->

<xsl:template match="source-profiles//operation">
  <operation>
    <xsl:call-template name="source-db-name-helper"/>
    <template><string><xsl:value-of select="."/></string></template>
  </operation>
</xsl:template>

<xsl:template match="target-profiles//operation">
  <xsl:choose>
    <xsl:when test="@type='standard-update'">
      <xsl:element name="{$std-update-operation-tag}">
        <xsl:apply-templates select="node()|@*"/>
      </xsl:element>
    </xsl:when>
    <xsl:otherwise>
      <operation>
        <xsl:call-template name="target-db-name-helper"/>
        <xsl:choose>
          <xsl:when test="@xml-as-text='yes'">
            <template><node><xsl:copy-of select="./node() | ./text()"/></node></template>
          </xsl:when>
          <xsl:otherwise>
            <template><string><xsl:value-of select="."/></string></template>
          </xsl:otherwise>
        </xsl:choose>
      </operation>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="operation">
  <operation>
    <string><xsl:value-of select="@db-name"/></string>
    <template><string><xsl:value-of select="."/></string></template>
  </operation>
</xsl:template>

<xsl:template match="terminate-if">
  <if>
    <comp-two-expr>
      <from-db>
        <xsl:call-template name="source-db-name-helper"/>
        <string><xsl:value-of select="$terminate-if-rs-name"/></string>
        <template><string><xsl:value-of select="."/></string></template>
      </from-db>
      <string-const><string><xsl:value-of select="@value"/></string></string-const>
    </comp-two-expr>
    <break><string><xsl:value-of select="$top-label"/></string></break>
  </if>
</xsl:template>

<xsl:template match="var-query">
    <xsl:choose>
      <xsl:when test="param">
        <map-var-query>
          <template><xsl:call-template name="source-db-name-helper"/></template>
          <map>
            <xsl:for-each select="param">
              <param key="{@name}"><template><string><xsl:value-of select="."/></string></template></param>
            </xsl:for-each>
          </map>
          <xsl:if test="@prefix">
            <template><string><xsl:value-of select="@prefix"/></string></template>
          </xsl:if>
        </map-var-query>
      </xsl:when>
      <xsl:otherwise>
        <var-query>
          <template><xsl:call-template name="source-db-name-helper"/></template>
          <template><string><xsl:value-of select="."/></string></template>
          <xsl:if test="@prefix">
            <template><string><xsl:value-of select="@prefix"/></string></template>
          </xsl:if>
        </var-query>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:call-template name="test-break-var"/>
</xsl:template>

<xsl:template match="query-xml">
    <xsl:variable name="record-tag">
      <template><string>
        <xsl:choose>
          <xsl:when test="@record-tag"><xsl:value-of select="@record-tag"/></xsl:when>
          <xsl:otherwise>record</xsl:otherwise>
        </xsl:choose>
      </string></template>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="param">
        <map-query-xml>
          <template><xsl:call-template name="source-db-name-helper"/></template>
          <map>
            <xsl:for-each select="param">
              <param key="{@name}"><template><string><xsl:value-of select="."/></string></template></param>
            </xsl:for-each>
          </map>
          <template><string><xsl:value-of select="@var"/></string></template>
          <xsl:copy-of select="$record-tag"/>
        </map-query-xml>
      </xsl:when>
      <xsl:otherwise>
        <query-xml>
          <template><xsl:call-template name="source-db-name-helper"/></template>
          <template><string><xsl:value-of select="."/></string></template>
          <template><string><xsl:value-of select="@var"/></string></template>
          <xsl:copy-of select="$record-tag"/>
        </query-xml>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:call-template name="test-break-var"/>
</xsl:template>

<xsl:template match="send-mail">
  <send-mail>
    <template><string><xsl:value-of select="@smtp-host"/></string></template>
    <template><string><xsl:value-of select="@from-addr"/></string></template>
    <template><string><xsl:value-of select="@from-name"/></string></template>
    <template><string><xsl:value-of select="@to"/></string></template>
    <template><string><xsl:value-of select="@subject"/></string></template>
    <template><string><xsl:value-of select="."/></string></template>
  </send-mail>
</xsl:template>

<xsl:template name="value-reaper">
  <template>
    <string>
      <xsl:choose>
        <xsl:when test="@value">
          <xsl:value-of select="@value"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="."/>
        </xsl:otherwise>
      </xsl:choose>
    </string>
  </template>
</xsl:template>

<xsl:template match="set-var">
  <set-var>
    <string><xsl:value-of select="@name"/></string>
    <xsl:call-template name="value-reaper"/>
  </set-var>
  <xsl:call-template name="test-break-var"/>
</xsl:template>

<xsl:template match="set-var-from-result-set">
  <rs-loop>
    <xsl:call-template name="source-db-name-helper"/>
    <string null="yes"/>
    <template><string><xsl:value-of select="query"/></string></template>
    <set-var>
      <string><xsl:value-of select="@var-name"/></string>
      <template><string><xsl:value-of select="template"/></string></template>
    </set-var>
    <string null="yes"/>
    <xsl:choose>
      <xsl:when test="@count-var-name">
        <string><xsl:value-of select="@count-var-name"/></string>
      </xsl:when>
      <xsl:otherwise>
        <string null="yes"/>
      </xsl:otherwise>
    </xsl:choose>
    <string null="yes"/>
  </rs-loop>
</xsl:template>

<xsl:template match="set-template">
  <set-template>
    <string><xsl:value-of select="@name"/></string>
    <xsl:call-template name="value-reaper"/>
  </set-template>
  <xsl:call-template name="test-break-var"/>
</xsl:template>

<xsl:template match="set-dyn-template">
  <set-dyn-template>
    <string><xsl:value-of select="@name"/></string>
    <xsl:call-template name="value-reaper"/>
  </set-dyn-template>
  <xsl:call-template name="test-break-var"/>
</xsl:template>

<xsl:template match="log-message">
  <log-message>
    <string>
      <xsl:choose>
        <xsl:when test="@severity">
          <xsl:value-of select="@severity"/>
        </xsl:when>
        <xsl:otherwise>info</xsl:otherwise>
      </xsl:choose>
    </string>
    <xsl:call-template name="value-reaper"/>
  </log-message>
</xsl:template>

<xsl:template match="log-data">
  <log-data>
    <string><xsl:value-of select="$system-params/@pipe-object-name"/></string>
    <xsl:call-template name="value-reaper"/>
  </log-data>
</xsl:template>

<xsl:template match="execute-if">
  <if>
    <xsl:choose>
      <xsl:when test="@var-name">
        <test-var>
          <string><xsl:value-of select="@var-name"/></string>
          <template><string><xsl:value-of select="@value"/></string></template>
        </test-var>
      </xsl:when>
      <xsl:when test="@regex">
        <regex>
          <template><string><xsl:value-of select="template"/></string></template>
          <string><xsl:value-of select="@regex"/></string>
          <xsl:choose>
            <xsl:when test="@re-prefix">
              <string><xsl:value-of select="@re-prefix"/></string>
            </xsl:when>
            <xsl:otherwise>
              <string null="yes"/>
            </xsl:otherwise>
          </xsl:choose>
        </regex>
      </xsl:when>
      <xsl:when test="@dyn-regex">
        <dyn-regex>
          <template><string><xsl:value-of select="template"/></string></template>
          <template><string><xsl:value-of select="@dyn-regex"/></string></template>
          <xsl:choose>
            <xsl:when test="@re-prefix">
              <string><xsl:value-of select="@re-prefix"/></string>
            </xsl:when>
            <xsl:otherwise>
              <string null="yes"/>
            </xsl:otherwise>
          </xsl:choose>
        </dyn-regex>
      </xsl:when>
      <xsl:when test="test">
        <xsl:apply-templates select="test/*"/>
      </xsl:when>
      <xsl:otherwise>
        <comp-two-expr>
          <template><string><xsl:value-of select="template1"/></string></template>
          <template><string><xsl:value-of select="template2"/></string></template>
        </comp-two-expr>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:call-template name="block-helper">
      <xsl:with-param name="block-content" select="then"/>
    </xsl:call-template>
    <xsl:if test="else">
      <xsl:call-template name="block-helper">
        <xsl:with-param name="block-content" select="else"/>
      </xsl:call-template>
    </xsl:if>
  </if>
</xsl:template>

<xsl:template match="switch">
  <case>
    <template>
      <xsl:choose>
        <xsl:when test="@template">
          <string><xsl:value-of select="@template"/></string>
        </xsl:when>
        <xsl:otherwise>
          <string><xsl:value-of select="template"/></string>
        </xsl:otherwise>
      </xsl:choose>
    </template>
    <map>
      <xsl:for-each select="case">
        <xsl:element name="entry">
          <xsl:attribute name="key"><xsl:value-of select="@value"/></xsl:attribute>
          <xsl:call-template name="block-helper">
            <xsl:with-param name="block-content" select="."/>
          </xsl:call-template>
        </xsl:element>
      </xsl:for-each>
    </map>
    <xsl:if test="else">
      <xsl:call-template name="block-helper">
        <xsl:with-param name="block-content" select="else"/>
      </xsl:call-template>
    </xsl:if>
  </case>
</xsl:template>

<xsl:template match="raise-error">
  <raise>
    <template><string><xsl:value-of select="."/></string></template>
  </raise>
</xsl:template>

<xsl:template match="catch-error">
  <catch>
    <xsl:call-template name="block-helper">
      <xsl:with-param name="block-content" select="execute"/>
    </xsl:call-template>
    <block><array>
      <xsl:apply-templates select="catch/*"/>
      <set-var>
        <string><xsl:value-of select="$error-var"/></string>
        <null-string/>
      </set-var>
    </array></block>
    <xsl:choose>
      <xsl:when test="@rethrow = 'yes'"><true/></xsl:when>
      <xsl:otherwise><not><true/></not></xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="@suppress-logging = 'yes'"><true/></xsl:when>
      <xsl:otherwise><not><true/></not></xsl:otherwise>
    </xsl:choose>
    <string><xsl:value-of select="$error-var"/></string>
  </catch>
</xsl:template>

<xsl:template match="stop-courier">
  <procedure-call>
    <string><xsl:value-of select="$system-params/@courier-object-name"/></string>
    <string><xsl:value-of select="$system-params/@courier-stop-stmt-name"/></string>
    <array>

      <xsl:choose>
        <xsl:when test="@timeout">
          <string><xsl:value-of select="@timeout"/></string>
        </xsl:when>
        <xsl:otherwise><string>30s</string></xsl:otherwise>
      </xsl:choose>
      
      <xsl:choose>
        <xsl:when test="@exit-status">
          <int><xsl:value-of select="@exit-status"/></int>
        </xsl:when>
      </xsl:choose>
      
    </array>
  </procedure-call>
</xsl:template>

<xsl:template match="launch-process">
  <launch-process>
    <string><xsl:value-of select="$system-params/@courier-object-name"/></string>
    <xsl:choose>
      <xsl:when test="@pipe-name"><template><string><xsl:value-of select="@pipe-name"/></string></template></xsl:when>
      <xsl:otherwise><null-string/></xsl:otherwise>
    </xsl:choose>    
    <xsl:choose>
      <xsl:when test="@rule-name"><template><string><xsl:value-of select="@rule-name"/></string></template></xsl:when>
      <xsl:otherwise><null-string/></xsl:otherwise>
    </xsl:choose>    
    <int><xsl:choose>
      <xsl:when test="@ignore-error-count"><xsl:value-of select="@ignore-error-count"/></xsl:when>
      <xsl:otherwise>0</xsl:otherwise>
    </xsl:choose></int>    
    <xsl:choose>
      <xsl:when test="script">
        <xsl:call-template name="block-helper">
          <xsl:with-param name="block-content" select="script"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="call-std-script"/>
      </xsl:otherwise>
    </xsl:choose>        
    <xsl:choose>
      <xsl:when test="@import-vars"><template><string><xsl:value-of select="@import-vars"/></string></template></xsl:when>
      <xsl:otherwise><null-string/></xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="@error-relaunch-interval">
        <string><xsl:value-of select="@relaunch-if-error-interval"/></string>
        <string><xsl:value-of select="@relaunch-if-error-limit"/></string>
      </xsl:when>
      <xsl:otherwise><string null="yes"/><string null="yes"/></xsl:otherwise>
    </xsl:choose>
  </launch-process>
</xsl:template>

<xsl:template match="exec-os-process">
  <exec-os-process>
    <string><xsl:value-of select="$system-params/@pipe-object-name"/></string>
    <template><string><xsl:value-of select="."/></string></template>
    <xsl:choose>
      <xsl:when test="@working-dir"><template><string><xsl:value-of select="@working-dir"/></string></template></xsl:when>
      <xsl:otherwise><null-string/></xsl:otherwise>
    </xsl:choose>    
    <xsl:choose>
      <xsl:when test="@log-output">
        <string><xsl:value-of select="@log-output"/></string>
      </xsl:when>
      <xsl:otherwise><string>yes</string></xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="@show-output">
        <string><xsl:value-of select="@show-output"/></string>
      </xsl:when>
      <xsl:otherwise><string>yes</string></xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="@no-error-stream-exception">
        <string><xsl:value-of select="@no-error-stream-exception"/></string>
      </xsl:when>
      <xsl:otherwise><string>no</string></xsl:otherwise>
    </xsl:choose>
  </exec-os-process>
</xsl:template>

<xsl:template match="exec-ssh-process">
  <exec-ssh-process>
    <string><xsl:value-of select="$system-params/@pipe-object-name"/></string>
    <xsl:choose>
      <xsl:when test="@host"><template><string><xsl:value-of select="@host"/></string></template></xsl:when>
      <xsl:otherwise><xsl:message terminate="yes">Mandatory attribute 'host' is missing</xsl:message></xsl:otherwise>
    </xsl:choose>    
    <xsl:choose>
      <xsl:when test="@port"><template><string><xsl:value-of select="@port"/></string></template></xsl:when>
      <xsl:otherwise><template><string>-1</string></template></xsl:otherwise>
    </xsl:choose>    
    <xsl:choose>
      <xsl:when test="@username"><template><string><xsl:value-of select="@username"/></string></template></xsl:when>
      <xsl:otherwise><xsl:message terminate="yes">Mandatory attribute 'username' is missing</xsl:message></xsl:otherwise>
    </xsl:choose>    
    <xsl:choose>
      <xsl:when test="@password"><template><string><xsl:value-of select="@password"/></string></template></xsl:when>
      <xsl:otherwise><xsl:message terminate="yes">Mandatory attribute 'password' is missing</xsl:message></xsl:otherwise>
    </xsl:choose>    
    <template><string><xsl:value-of select="."/></string></template>
    <xsl:choose>
      <xsl:when test="@log-output">
        <string><xsl:value-of select="@log-output"/></string>
      </xsl:when>
      <xsl:otherwise><string>yes</string></xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="@show-output">
        <string><xsl:value-of select="@show-output"/></string>
      </xsl:when>
      <xsl:otherwise><string>yes</string></xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="@no-error-stream-exception">
        <string><xsl:value-of select="@no-error-stream-exception"/></string>
      </xsl:when>
      <xsl:otherwise><string>no</string></xsl:otherwise>
    </xsl:choose>
  </exec-ssh-process>
</xsl:template>

<xsl:template match="stop-process">
  <stop-process/>
</xsl:template>


<!-- *************************************************************************** -->
<!-- *************************************************************************** -->
<!-- *************************************************************************** -->

<xsl:template name="test-break-var">
  <if>
    <test-var>
      <string><xsl:value-of select="$break-var"/></string>
      <string>1</string>
    </test-var>
    <break><string><xsl:value-of select="$top-label"/></string></break>
  </if>
</xsl:template>

<xsl:template match="external-file">
  <xsl:apply-templates select="document(@name)"/>
</xsl:template>

<xsl:template match="external-file-tags">
  <xsl:apply-templates select="document(@name)/*/*"/>
</xsl:template>

<xsl:template name="sybase">
  <source
    type="sybase"
    check-sql="SELECT GetDate()"
  >
    <xsl:apply-templates select="*|@*"/>
  </source>
</xsl:template>

<xsl:template match="db-profiles/sybase">
  <xsl:call-template name="sybase"/>
</xsl:template>

<xsl:template name="mssql">
  <source
    type="mssql" 
    check-sql="SELECT GetDate()"
  >
    <xsl:apply-templates select="*|@*"/>
  </source>
</xsl:template>

<xsl:template match="db-profiles/mssql">
  <xsl:call-template name="mssql"/>
</xsl:template>

</xsl:stylesheet>