[英文版](README.md) | [中国語版](README_CN.md)

# SuperSonic

**SuperSonicは、LLM（大規模言語モデル）によるChat BIと、セマンティックレイヤーによるHeadless BIを統合した次世代のBIプラットフォームです。** この統合により、Chat BIは従来のBIと同様に、統一されたガバナンスされたセマンティックデータモデルにアクセスできます。さらに、両方のBIパラダイムは統合から恩恵を受けます：

- Chat BIのText2SQLは、セマンティックモデルからのコンテキスト検索によって強化されます。
- Headless BIのクエリインターフェースは、自然言語APIによって拡張されます。

![SuperSonicのアイデア](https://github.com/supersonicbi/supersonic-website/blob/main/static/img/supersonic_ideas.png)

SuperSonicは、ユーザーが自然言語でデータをクエリし、適切なチャートで結果を視覚化できる**Chat BIインターフェース**を提供します。このような体験を実現するために必要なのは、**Headless BIインターフェース**を通じて論理的なセマンティックモデル（メトリック/ディメンション/タグの定義、それらの意味と関係など）を構築することだけです。同時に、SuperSonicは拡張可能で構成可能な設計を採用しており、Java SPIを使用してカスタム実装を追加および設定できます。

![SuperSonicのデモ](https://github.com/supersonicbi/supersonic-website/blob/main/static/img/supersonic_demo.gif)

## プロジェクトの動機

ChatGPTのような大規模言語モデル（LLM）の出現は、情報検索の方法を再定義し、データ分析分野における新しいパラダイムであるChat BIをリードしています。Chat BIを実装するために、学術界と産業界は主に、自然言語をSQLに変換するLLMの能力を活用することに焦点を当てています。これは一般にText2SQLまたはNL2SQLと呼ばれます。一部のアプローチは有望な結果を示していますが、大規模な実世界のアプリケーションでの**信頼性**はまだ不十分です。

一方で、統一されたセマンティックデータモデルを構築することに焦点を当てた別の新興パラダイムであるHeadless BIも、大きな注目を集めています。Headless BIは、オープンAPIを介して一貫したデータセマンティクスを公開するユニバーサルセマンティックレイヤーを通じて実装されます。

私たちの観点から見ると、Chat BIとHeadless BIの統合は、Text2SQL生成を2つの側面で強化する可能性があります：

1. データセマンティクス（ビジネス用語、列の値など）をプロンプトに組み込むことで、LLMがセマンティクスをよりよく理解し、**幻覚を減らす**ことができます。
2. 高度なSQL構文（結合、式など）の生成をLLMからセマンティックレイヤーにオフロードすることで、**複雑さを減らす**ことができます。

これらのアイデアを念頭に置いて、私たちはSuperSonicプロジェクトを開発し、実際の製品でそれを使用しています。同時に、SuperSonicを拡張可能なフレームワークとしてオープンソース化し、データクエリ対話分野のさらなる発展を促進したいと考えています。

## 初期設定で利用可能な機能

- *ビジネスユーザー*が自然言語クエリを入力できる組み込みのChat BIインターフェース。
- *分析エンジニア*がセマンティックモデルを構築できる組み込みのHeadless BIインターフェース。
- 特定のシナリオ（例：デモンストレーション、統合テスト）で推論効率を向上させるための組み込みのルールベースのセマンティックパーサー。
- 入力自動補完、マルチターン会話、クエリ後の質問推奨などの高度な機能をサポート。
- データセットレベル、列レベル、行レベルの3レベルのデータアクセス制御をサポート。

## 拡張可能なコンポーネント

高レベルのアーキテクチャとメインのプロセスフローは以下の通りです：

![SuperSonicのコンポーネント](https://github.com/supersonicbi/supersonic-website/blob/main/static/img/supersonic_components.png)

- **モデル知識ベース(Knowledge Base)：** セマンティックモデルから定期的にスキーマ情報を抽出し、辞書とインデックスを構築して、スキーママッピングを容易にします。

- **スキーママッパー(Schema Mapper)：** ユーザークエリ内のスキーマ要素（メトリック/ディメンション/エンティティ/値）を識別します。クエリテキストを知識ベースと照合します。

- **セマンティックパーサー(Semantic Parser)：** ユーザークエリを理解し、セマンティッククエリステートメントS2SQLを生成します。

- **セマンティック修正器(Semantic Corrector)：** セマンティッククエリステートメントの妥当性をチェックし、必要に応じて修正と最適化を行います。

- **セマンティックトランスレーター(Semantic Translator)：** セマンティッククエリステートメントを、物理データモデル上で実行可能なSQLステートメントに変換します。

- **チャットプラグイン(Chat Plugin)：** サードパーティツールで機能を拡張します。すべての設定されたプラグインとその機能説明、サンプル質問が与えられた場合、LLMは最も適切なプラグインを選択します。

## クイックデモ
### オンラインプレイグラウンド
http://117.72.46.148:9080 にアクセスして、新規ユーザーとして登録して体験してください。システム設定を変更しないでください。毎週末に定期的に再起動して設定をリセットします。

### ローカルビルド
SuperSonicには、サンプルのセマンティックモデルとチャット会話が付属しており、以下の手順で簡単に体験できます：

- [リリースページ](https://github.com/tencentmusic/supersonic/releases)から最新のプリビルドバイナリをダウンロード
- スクリプト "assembly/bin/supersonic-daemon.sh start" を実行して、スタンドアロンJavaサービスを起動
- ブラウザで http://localhost:9080 にアクセスして探索を開始

## ビルドと開発

プロジェクト[ドキュメント](https://supersonicbi.github.io/docs/%E7%B3%BB%E7%BB%9F%E9%83%A8%E7%BD%B2/%E7%BC%96%E8%AF%91%E6%9E%84%E5%BB%BA/)を参照してください。

## WeChat連絡先

SuperSonicの公式WeChatアカウントをフォローしてください：

![SuperSonicのWeChat公式アカウント](https://github.com/supersonicbi/supersonic-website/blob/main/static/img/supersonic_wechat_oa.png)

WeChatコミュニティに参加することを歓迎します：

![SuperSonicのWeChatコミュニティ](https://github.com/supersonicbi/supersonic-website/blob/main/static/img/supersonic_wechat.png)
