inherited DataGridFrame1: TDataGridFrame1
  Width = 424
  Height = 324
  object RzToolbar1: TRzToolbar
    Left = 0
    Top = 25
    Width = 424
    Height = 34
    AutoStyle = False
    Margin = 0
    TopMargin = 0
    RowHeight = 34
    ButtonHeight = 60
    TextOptions = ttoCustom
    BorderInner = fsNone
    BorderOuter = fsGroove
    BorderSides = [sdBottom]
    BorderWidth = 0
    TabOrder = 0
    ToolbarControls = (
      RzToolbar5)
    object RzToolbar5: TRzToolbar
      Left = 0
      Top = 6
      Width = 76
      Height = 23
      AutoStyle = False
      Margin = 0
      TopMargin = 0
      RowHeight = 23
      WrapControls = False
      BorderInner = fsNone
      BorderOuter = fsFlatRounded
      BorderSides = [sdRight]
      BorderWidth = 0
      TabOrder = 0
      ToolbarControls = (
        RzSpacer4
        btnCalculation)
      object RzSpacer4: TRzSpacer
        Left = 0
        Top = -1
        Width = 4
      end
      object btnCalculation: TRzBitBtn
        Left = 4
        Top = -1
        Width = 69
        Hint = 'Return collateral'
        Caption = 'Return'
        Enabled = False
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clWindowText
        Font.Height = -11
        Font.Name = 'MS Sans Serif'
        Font.Style = [fsBold]
        HotTrack = True
        ParentFont = False
        ParentShowHint = False
        ShowHint = True
        TabOrder = 0
      end
    end
  end
  object RzPanel1: TRzPanel
    Left = 0
    Top = 0
    Width = 424
    Height = 25
    Align = alTop
    BorderOuter = fsGroove
    BorderSides = [sdBottom]
    Caption = 'Collateral'
    Font.Charset = DEFAULT_CHARSET
    Font.Color = clWindowText
    Font.Height = -16
    Font.Name = 'MS Sans Serif'
    Font.Style = [fsBold]
    ParentFont = False
    TabOrder = 1
  end
  object grid: TStepinDataGrid
    Left = 0
    Top = 59
    Width = 424
    Height = 265
    Active = False
    Align = alClient
    AutoFitColWidths = True
    Columns = <
      item
        EditButtons = <>
        FieldName = 'InstrumentCode'
        Footers = <>
        Title.Caption = 'Instrument'
        Width = 113
      end
      item
        AutoFitColWidth = False
        DisplayFormat = ',0'
        EditButtons = <>
        FieldName = 'FreeQuantity'
        Footers = <>
        Title.Caption = 'Free Quantity'
        Width = 80
      end
      item
        AutoFitColWidth = False
        DisplayFormat = ',0.####'
        EditButtons = <>
        FieldName = 'Price'
        Footers = <>
        Width = 80
      end
      item
        AutoFitColWidth = False
        DisplayFormat = ',0'
        EditButtons = <>
        FieldName = 'Amount'
        Footer.DisplayFormat = ',0'
        Footer.ValueType = fvtSum
        Footers = <>
        Width = 80
      end
      item
        Alignment = taCenter
        AutoFitColWidth = False
        EditButtons = <>
        FieldName = 'LastUpdate'
        Footers = <>
        Title.Caption = 'Last Update'
        Width = 69
      end>
    DateFormat = 'dd mmm yy'
    FooterRowCount = 1
    GridFont.Charset = DEFAULT_CHARSET
    GridFont.Color = clWindowText
    GridFont.Height = -11
    GridFont.Name = 'MS Sans Serif'
    GridFont.Style = []
    GridFooterFont.Charset = DEFAULT_CHARSET
    GridFooterFont.Color = clWindowText
    GridFooterFont.Height = -11
    GridFooterFont.Name = 'MS Sans Serif'
    GridFooterFont.Style = [fsBold]
    QueryProvider = DatabaseServices.queryProviderOptionBoard
    SQL.Strings = (
      'exec poptGetMainAccount @Counterpart_Id = 1000')
  end
end
