<?xml version="1.0" encoding="UTF-8"?>
<!--
 File:        $Id$
 Revision:    $Revision$
 Author:      $Author$
 Date:        $Date$

 The Netarchive Suite - Software to harvest and preserve websites
 Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, 
 MA  02110-1301  USA
 -->
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:dk="http://www.netarkivet.dk/schemas/settings"
xmlns="http://www.netarkivet.dk/schemas/settings"
exclude-result-prefixes="dk">

    <!-- This script updates settings.xml files from versions before 3.3.3,
     adding a setting telling the harvester the minimum amount of space
     required to accept a crawl job.
    -->

<xsl:output method="xml" encoding="UTF-8" />

    <xsl:template xml:space="preserve" match="dk:harvesting/dk:serverDir"><!--
    --><xsl:copy><xsl:copy-of select="@*"/><xsl:apply-templates/></xsl:copy>
                <xsl:comment>The minimum amount of free bytes in the serverDir
                required before accepting any harvest-jobs. Default is 
                 400000000 bytes (~400 Mbytes)</xsl:comment>
                 <minSpaceLeft>400000000</minSpaceLeft>
    </xsl:template>

<!-- Any other node gets copied unchanged. Don't change this. -->
<xsl:template match="*">
	<xsl:copy>
		<xsl:copy-of select="@*"/>
		<xsl:apply-templates/>
	</xsl:copy>
</xsl:template>

<!-- Please keep the comments around -->
    <xsl:template match="comment()">
        <xsl:copy/>
    </xsl:template>

</xsl:stylesheet>
           
